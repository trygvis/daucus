package io.trygvis.esper.testing.gitorious;

import com.jolbox.bonecp.*;
import fj.data.*;
import static fj.data.Option.*;
import io.trygvis.esper.testing.*;
import static java.lang.System.*;
import org.apache.abdera.parser.*;
import org.apache.commons.httpclient.protocol.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class GitoriousImporter {
    private final BoneCP boneCp;
    private final GitoriousClient gitoriousClient;

    public static void main(String[] args) throws Exception {
        Config config = Config.loadFromDisk();
        new GitoriousImporter(config, DbMain.JDBC_URL, "esper", "esper");
    }

    public GitoriousImporter(Config config, final String jdbcUrl, final String jdbcUsername, final String jdbcPassword) throws Exception {
        BoneCPConfig boneCPConfig = new BoneCPConfig(){{
            setJdbcUrl(jdbcUrl);
            setUsername(jdbcUsername);
            setPassword(jdbcPassword);
            setDefaultAutoCommit(false);
            setMaxConnectionsPerPartition(10);
        }};

        boneCp = new BoneCP(boneCPConfig);

        gitoriousClient = new GitoriousClient(HttpClient.createHttpClient(config), config.gitoriousUrl);

        final ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(2);

        boolean projectsUpdateEnabled = true;
        int projectsUpdateDelay = 0 * 1000;
        int projectsUpdateInterval = 60 * 1000;

        boolean repositoriesUpdateEnabled = false;
        int repositoriesUpdateDelay = 0;
        int repositoriesUpdateInterval = 60 * 1000;

        if (projectsUpdateEnabled) {
            service.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        discoverProjects();
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                }
            }, projectsUpdateDelay, projectsUpdateInterval, TimeUnit.MILLISECONDS);
        }

        if (repositoriesUpdateEnabled) {
            service.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        updateRepositories();
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                }
            }, repositoriesUpdateDelay, repositoriesUpdateInterval, TimeUnit.MILLISECONDS);
        }
    }

    private void discoverProjects() throws Exception {
        Set<GitoriousProjectXml> projects = gitoriousClient.findProjects();

        long start = currentTimeMillis();
        try (Daos daos = Daos.lookup(boneCp)) {
            GitoriousRepositoryDao repoDao = daos.gitoriousRepositoryDao;
            GitoriousProjectDao projectDao = daos.gitoriousProjectDao;

            System.out.println("Processing " + projects.size() + " projects.");
            for (GitoriousProjectXml project : projects) {
                if (projectDao.countProjects(project.slug) == 0) {
                    System.out.println("New project: " + project.slug + ", has " + project.repositories.size() + " repositories.");
                    projectDao.insertProject(project.slug);
                    for (GitoriousRepositoryXml repository : project.repositories) {
                        URI atomFeed = gitoriousClient.atomFeed(repository.projectSlug, repository.name);
                        repoDao.insertRepository(repository.projectSlug, repository.name, atomFeed);
                    }
                } else {
                    for (GitoriousRepositoryXml repository : project.repositories) {
                        if (repoDao.countRepositories(repository.projectSlug, repository.name) == 0) {
                            System.out.println("New repository for project " + repository.projectSlug + ": " + repository.name);
                            URI atomFeed = gitoriousClient.atomFeed(repository.projectSlug, repository.name);
                            repoDao.insertRepository(repository.projectSlug, repository.name, atomFeed);
                        }
                    }

                    for (GitoriousRepository repository : repoDao.selectForProject(project.slug)) {
                        boolean found = false;
                        for (GitoriousRepositoryXml repo : project.repositories) {
                            if (repo.projectSlug.equals(repository.projectSlug) && repo.name.equals(repository.name)) {
                                found = true;
                                break;
                            }
                        }

                        if (found) {
                            continue;
                        }

                        System.out.println("Gone repository for project " + repository.projectSlug + ": " + repository.name);
                        repoDao.delete(repository);
                    }
                }
            }

            for (String project : projectDao.selectSlugs()) {
                boolean found = false;
                for (GitoriousProjectXml p : projects) {
                    if (p.slug.equals(project)) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    continue;
                }

                System.out.println("Gone project: " + project);
                repoDao.deleteForProject(project);
                projectDao.delete(project);
            }

            daos.commit();
        }
        long end = currentTimeMillis();
        System.out.println("Processed in " + (end - start) + " ms");
    }

    private void updateRepositories() throws SQLException, IOException {
        try (Daos daos = Daos.lookup(boneCp)) {
            List<GitoriousRepository> list = daos.gitoriousRepositoryDao.select();
            System.out.println("Updating " + list.size() + " feeds.");
            for (GitoriousRepository repository : list) {
                updateFeed(daos, repository);
                daos.commit();
            }
        }
    }

    private void updateFeed(Daos daos, GitoriousRepository repository) throws SQLException {
        GitoriousRepositoryDao repositoryDao = daos.gitoriousRepositoryDao;
        GitoriousEventDao eventDao = daos.gitoriousEventDao;

        Option<Date> lastUpdate = repository.lastSuccessfulUpdate;

        Iterable<GitoriousEvent> events;
        try {
            events = gitoriousClient.fetchGitoriousEvents(repository, lastUpdate);
        } catch (ParseException e) {
            repositoryDao.updateTimestamp(repository.projectSlug, repository.name, new Timestamp(currentTimeMillis()), Option.<Date>none());
            System.out.println("Error parsing " + repository.atomFeed);
            e.printStackTrace(System.out);
            return;
        }

        for (GitoriousEvent event : events) {
            if (eventDao.countEntryId(event.entryId) == 0) {
                System.out.println("New entry in " + repository.atomFeed + ": " + event.entryId);
                eventDao.insertEvent(event);
            } else {
                System.out.println("Already imported entry: " + event.entryId);
            }
        }

        repositoryDao.updateTimestamp(repository.projectSlug, repository.name, new Timestamp(currentTimeMillis()), some(new Date()));
    }
}

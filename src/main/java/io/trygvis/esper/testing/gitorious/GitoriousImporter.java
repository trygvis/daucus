package io.trygvis.esper.testing.gitorious;

import com.jolbox.bonecp.*;
import fj.data.*;
import static fj.data.Option.*;
import io.trygvis.esper.testing.*;
import static java.lang.System.*;
import org.apache.abdera.*;
import org.apache.abdera.model.*;
import org.apache.abdera.parser.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.client.*;

import java.io.*;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class GitoriousImporter {
    private final Parser parser;
    private final BoneCP boneCp;
    private final GitoriousClient gitoriousClient;
    private final HTTPCache httpCache;

    public static void main(String[] args) throws Exception {
        Main.configureLog4j();
        new GitoriousImporter(DbMain.JDBC_URL, "esper", "");
    }

    public GitoriousImporter(String jdbcUrl, String jdbcUsername, String jdbcPassword) throws Exception {
        Abdera abdera = new Abdera();
        parser = abdera.getParser();

        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(jdbcUsername);
        config.setPassword(jdbcPassword);
        config.setDefaultAutoCommit(false);
        config.setMaxConnectionsPerPartition(10);

        boneCp = new BoneCP(config);

        httpCache = new HTTPCache(new MemoryCacheStorage(), HTTPClientResponseResolver.createMultithreadedInstance());

        gitoriousClient = new GitoriousClient(httpCache, "http://gitorious.org");

        final ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);

        int projectsUpdateDelay = 0 * 1000;
        int projectsUpdateInterval = 60 * 1000;
        int repositoriesUpdateDelay = 0;
        int repositoriesUpdateInterval = 60 * 1000;

        service.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    discoverProjects();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        }, projectsUpdateDelay, projectsUpdateInterval, TimeUnit.MILLISECONDS);

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
                        repoDao.insertRepository(repository.projectSlug, repository.name, gitoriousClient.atomFeed(project.slug));
                    }
                } else {
                    for (GitoriousRepositoryXml repository : project.repositories) {
                        if (repoDao.countRepositories(repository.projectSlug, repository.name) == 0) {
                            System.out.println("New repository for project " + repository.projectSlug + ": " + repository.name);
                            repoDao.insertRepository(repository.projectSlug, repository.name, gitoriousClient.atomFeed(project.slug));
                        }
                    }

                    for (GitoriousRepository repository : repoDao.selectForProject(project.slug)) {
                        boolean found = false;
                        for (Iterator<GitoriousRepositoryXml> it = project.repositories.iterator(); it.hasNext(); ) {
                            GitoriousRepositoryXml repo = it.next();
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
                for (Iterator<GitoriousProjectXml> it = projects.iterator(); it.hasNext(); ) {
                    GitoriousProjectXml p = it.next();
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

        Option<Date> lastUpdate = repository.lastUpdate;

        System.out.println("Fetching " + repository.atomFeed);

        long start = currentTimeMillis();
        HTTPResponse response = httpCache.execute(new HTTPRequest(repository.atomFeed, HTTPMethod.GET));
        long end = currentTimeMillis();
        System.out.println("Fetched in " + (end - start) + "ms");

        // Use the server's timestamp
        Date responseDate = response.getDate().toDate();

        System.out.println("responseDate = " + responseDate);

        Document<Element> document;
        try {
            document = parser.parse(response.getPayload().getInputStream());
        } catch (ParseException e) {
            repositoryDao.updateTimestamp(repository.projectSlug, repository.name, new Timestamp(currentTimeMillis()), Option.<Date>none());
            System.out.println("Error parsing " + repository.atomFeed);
            e.printStackTrace(System.out);
            return;
        }

        Feed feed = (Feed) document.getRoot();

        for (Entry entry : feed.getEntries()) {
            String entryId = entry.getId().toASCIIString();
            Date published = entry.getPublished();
            String title = entry.getTitle();

            // Validate element
            if (entryId == null || published == null || title == null) {
                continue;
            }

            if (lastUpdate.isSome() && lastUpdate.some().after(published)) {
                System.out.println("Old entry: " + repository.atomFeed + ":" + entryId);
                continue;
            }

            if (eventDao.countEntryId(entryId) == 0) {
                System.out.println("New entry: " + repository.atomFeed + ":" + entryId);
                eventDao.insertChange(entryId, title);
            } else {
                System.out.println("Already imported entry: " + entryId);
            }
        }

        repositoryDao.updateTimestamp(repository.projectSlug, repository.name, new Timestamp(currentTimeMillis()), some(new Date()));
    }
}

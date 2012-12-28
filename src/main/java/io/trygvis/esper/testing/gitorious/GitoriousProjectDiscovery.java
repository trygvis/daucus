package io.trygvis.esper.testing.gitorious;

import com.jolbox.bonecp.*;
import fj.data.Option;
import static fj.data.Option.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.util.object.ActorRef;
import io.trygvis.esper.testing.util.object.ObjectFactory;
import io.trygvis.esper.testing.util.object.ObjectManager;
import io.trygvis.esper.testing.util.object.ObjectUtil;
import io.trygvis.esper.testing.util.object.TransactionalActor;

import io.trygvis.esper.testing.util.*;
import static java.lang.System.*;
import org.apache.abdera.parser.*;

import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;

public class GitoriousProjectDiscovery {
    private final BoneCPDataSource boneCp;
    private final GitoriousClient gitoriousClient;

    public static void main(String[] args) throws Exception {
        Config config = Config.loadFromDisk("gitorious-project-discovery");
        new GitoriousProjectDiscovery(config);
    }

    public GitoriousProjectDiscovery(final Config config) throws Exception {
        boneCp = config.createBoneCp();
        GitoriousAtomFeedParser parser = new GitoriousAtomFeedParser(config.createAbdera());
        gitoriousClient = new GitoriousClient(HttpClient.createHttpCache(config), config.gitorious.url, parser);

        final ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);

        Set<GitoriousRepositoryDto> repositories = Collections.emptySet();
        try (Connection c = boneCp.getConnection()) {
            repositories = new HashSet<>(new Daos(c).gitoriousRepositoryDao.select(Daos.OrderDirection.ASC));
        } catch (SQLException e) {
            // ignore
        }

        final ObjectManager<GitoriousRepositoryDto, ActorRef<GitoriousRepository>> repositoryManager = new ObjectManager<>("", repositories, new ObjectFactory<GitoriousRepositoryDto, ActorRef<GitoriousRepository>>() {
            public ActorRef<GitoriousRepository> create(GitoriousRepositoryDto repository) {
                return ObjectUtil.scheduledActorWithFixedDelay(service, 0, 60, TimeUnit.SECONDS, boneCp, "Gitorious", new GitoriousRepository(gitoriousClient, repository));
            }
        });

        ObjectUtil.scheduledActorWithFixedDelay(service, config.gitorious.projectListUpdateDelay, config.gitorious.projectListUpdateInterval, TimeUnit.MILLISECONDS, boneCp, "Gitorious", new TransactionalActor() {
            public void act(Connection c) throws Exception {
                try (Daos daos = new Daos(c)) {
                    discoverProjects(daos);
                    repositoryManager.update(daos.gitoriousRepositoryDao.select(Daos.OrderDirection.NONE));
                    daos.commit();
                }
            }
        });
    }

    private void discoverProjects(Daos daos) throws Exception {
        Set<GitoriousProjectXml> projects = gitoriousClient.findProjects();

        long start = currentTimeMillis();
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

                for (GitoriousRepositoryDto repository : repoDao.selectForProject(project.slug)) {
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

        long end = currentTimeMillis();
        System.out.println("Processed in " + (end - start) + " ms");
    }
}

class GitoriousRepository implements TransactionalActor {
    private final GitoriousClient gitoriousClient;
    private final GitoriousRepositoryDto repository;

    public GitoriousRepository(GitoriousClient gitoriousClient, GitoriousRepositoryDto repository) {
        this.gitoriousClient = gitoriousClient;
        this.repository = repository;
    }

    public void act(Connection c) throws Exception {
        Daos daos = new Daos(c);
        updateFeed(daos, repository);
        c.commit();
    }

    private void updateFeed(Daos daos, GitoriousRepositoryDto repository) throws SQLException {
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

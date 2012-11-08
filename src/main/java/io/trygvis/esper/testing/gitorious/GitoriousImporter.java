package io.trygvis.esper.testing.gitorious;

import com.jolbox.bonecp.*;
import com.jolbox.bonecp.hooks.*;
import io.trygvis.esper.testing.*;
import org.apache.abdera.*;
import org.apache.abdera.protocol.client.*;
import org.apache.abdera.protocol.client.cache.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.client.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class GitoriousImporter {
//    private final AbderaClient abderaClient;
    private final BoneCP boneCp;
    private final GitoriousClient gitoriousClient;

    public static void main(String[] args) throws Exception {
        Main.configureLog4j();
        new GitoriousImporter();
    }

    public GitoriousImporter() throws Exception {
        Abdera abdera = new Abdera();
//        abderaClient = new AbderaClient(abdera, new LRUCache(abdera, 1000));

        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl(DbMain.JDBC_URL);
        config.setUsername("esper");
        config.setPassword("");
        config.setDefaultAutoCommit(false);
        config.setMaxConnectionsPerPartition(1);

        config.setConnectionHook(new AbstractConnectionHook() {
            public void onAcquire(ConnectionHandle c) {
                try {
                    c.setDebugHandle(new Daos(c));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        boneCp = new BoneCP(config);

        HTTPCache httpCache = new HTTPCache(new MemoryCacheStorage(), HTTPClientResponseResolver.createMultithreadedInstance());

        gitoriousClient = new GitoriousClient(httpCache, "https://gitorious.org");

        final ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);

        int projectsUpdateInterval = 1000;
        final int projectUpdateInterval = 1000;

//        service.scheduleAtFixedRate(new Runnable() {
//            public void run() {
//                try {
//                    discoverProjects();
//                } catch (Exception e) {
//                    e.printStackTrace(System.out);
//                }
//            }
//        }, projectsUpdateInterval, projectsUpdateInterval, TimeUnit.MILLISECONDS);

        discoverProjects();
    }

    private void discoverProjects() throws Exception {
        Set<GitoriousProject> projects = gitoriousClient.findProjects();

        try (ConnectionHandle connection = (ConnectionHandle) boneCp.getConnection()) {
            Daos daos = (Daos) connection.getDebugHandle();
            GitoriousRepositoryDao repoDao = daos.gitoriousRepositoryDao;
            GitoriousProjectDao projectDao = daos.gitoriousProjectDao;

            daos.begin();
            System.out.println("Processing " + projects.size() + " projects.");
            for (GitoriousProject project : projects) {
                if(projectDao.countProjects(project.slug) == 0) {
                    System.out.println("New project: " + project.slug + ", has " + project.repositories.size() + " repositories.");
                    projectDao.insertProject(project);
                    for (GitoriousRepository repository : project.repositories) {
                        repoDao.insertRepository(repository);
                    }
                }
                else {
                    for (GitoriousRepository repository : project.repositories) {
                        if(repoDao.countRepositories(repository) == 0) {
                            System.out.println("New repository for project " + repository.projectSlug + ": " + repository.name);
                            repoDao.insertRepository(repository);
                        }
                    }

                    for (GitoriousRepository repository : repoDao.selectForProject(project.slug)) {
                        if(project.repositories.contains(repository)) {
                            continue;
                        }
                        System.out.println("Gone repository for project " + repository.projectSlug + ": " + repository.name);
                        repoDao.delete(repository);
                    }
                }
            }

            for (String project : projectDao.selectAll()) {
                boolean found = false;
                for (Iterator<GitoriousProject> it = projects.iterator(); it.hasNext(); ) {
                    GitoriousProject p = it.next();
                    if(p.slug.equals(project)) {
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

            connection.commit();
        }
    }

    /*
    private void work() throws SQLException, InterruptedException {
        String url = "http://qt.gitorious.org/projects/show/qt.atom";

        while (true) {
            Timestamp lastUpdate = atomDao.getAtomFeed(url);

            System.out.println("Fetching " + url);
            RequestOptions options = new RequestOptions();
            if (lastUpdate != null) {
                options.setIfModifiedSince(lastUpdate);
            }

            long start = System.currentTimeMillis();
            ClientResponse response = abderaClient.get(url, options);
            long end = System.currentTimeMillis();
            System.out.println("Fetched in " + (end - start) + "ms");

            // Use the server's timestamp
            Date responseDate = response.getDateHeader("Date");

            System.out.println("responseDate = " + responseDate);

            Document<Element> document = response.getDocument();
            Feed feed = (Feed) document.getRoot();

            for (Entry entry : feed.getEntries()) {
                String entryId = entry.getId().toASCIIString();
                Date published = entry.getPublished();
                String title = entry.getTitle();

                // Validate element
                if (entryId == null || published == null || title == null) {
                    continue;
                }

                if (lastUpdate != null && lastUpdate.after(published)) {
                    System.out.println("Old entry: " + url + ":" + entryId);
                    continue;
                }

                System.out.println("New entry: " + url + ":" + entryId);
                if (gitoriousDao.countEntryId(entryId) == 0) {
                    gitoriousDao.insertChange(entryId, title);
                } else {
                    System.out.println("Already imported entry: " + entryId);
                }
            }

            if (lastUpdate == null) {
                System.out.println("New atom feed");
                atomDao.insertAtomFeed(url, new Timestamp(responseDate.getTime()));
            } else {
                System.out.println("Updating atom feed");
                atomDao.updateAtomFeed(url, lastUpdate);
            }

            connection.commit();

            System.out.println("Sleeping");
            Thread.sleep(10 * 1000);
        }
    }
    */
}

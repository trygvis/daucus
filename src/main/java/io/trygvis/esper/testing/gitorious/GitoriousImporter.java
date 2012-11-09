package io.trygvis.esper.testing.gitorious;

import com.jolbox.bonecp.*;
import fj.*;
import io.trygvis.esper.testing.*;
import static java.lang.System.*;
import org.apache.abdera.*;
import org.apache.abdera.model.*;
import org.apache.abdera.parser.*;
import org.apache.abdera.protocol.client.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.client.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.*;

public class GitoriousImporter {
    private final Parser parser;
    private final BoneCP boneCp;
    private final GitoriousClient gitoriousClient;
    private final HTTPCache httpCache;

    public static void main(String[] args) throws Exception {
        Main.configureLog4j();
        new GitoriousImporter();
    }

    public GitoriousImporter() throws Exception {
        Abdera abdera = new Abdera();
        parser = abdera.getParser();

        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl(DbMain.JDBC_URL);
        config.setUsername("esper");
        config.setPassword("");
        config.setDefaultAutoCommit(false);
        config.setMaxConnectionsPerPartition(10);

//        config.setConnectionHook(new AbstractConnectionHook() {
//            public void onAcquire(ConnectionHandle c) {
//                try {
//                    System.out.println("New SQL connection.");
//                    c.setDebugHandle(new Daos(c));
//                } catch (SQLException e) {connections
//                    throw new RuntimeException(e);
//                }
//            }
//        });

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
        Set<GitoriousProject> projects = gitoriousClient.findProjects();

        long start = currentTimeMillis();
        try (Daos daos = Daos.lookup(boneCp)) {
            GitoriousRepositoryDao repoDao = daos.gitoriousRepositoryDao;
            GitoriousProjectDao projectDao = daos.gitoriousProjectDao;

            System.out.println("Processing " + projects.size() + " projects.");
            for (GitoriousProject project : projects) {
                if (projectDao.countProjects(project.slug) == 0) {
                    System.out.println("New project: " + project.slug + ", has " + project.repositories.size() + " repositories.");
                    projectDao.insertProject(project);
                    for (GitoriousRepository repository : project.repositories) {
                        repoDao.insertRepository(repository);
                    }
                } else {
                    for (GitoriousRepository repository : project.repositories) {
                        if (repoDao.countRepositories(repository) == 0) {
                            System.out.println("New repository for project " + repository.projectSlug + ": " + repository.name);
                            repoDao.insertRepository(repository);
                        }
                    }

                    for (GitoriousRepository repository : repoDao.selectForProject(project.slug)) {
                        if (project.repositories.contains(repository)) {
                            continue;
                        }
                        System.out.println("Gone repository for project " + repository.projectSlug + ": " + repository.name);
                        repoDao.delete(repository);
                    }
                }
            }

            for (String project : projectDao.selectSlugs()) {
                boolean found = false;
                for (Iterator<GitoriousProject> it = projects.iterator(); it.hasNext(); ) {
                    GitoriousProject p = it.next();
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
            List<P2<String, URI>> list = daos.gitoriousProjectDao.selectFeeds();
            System.out.println("Updating " + list.size() + " feeds.");
            for (P2<String, URI> pair : list) {
                updateFeed(daos, pair._1(), pair._2());
                daos.commit();
            }
        }
    }

    private void updateFeed(Daos daos, String slug, URI uri) throws SQLException {
        AtomDao atomDao = daos.atomDao;
        GitoriousEventDao eventDao = daos.gitoriousEventDao;

        Timestamp lastUpdate = atomDao.getAtomFeed(uri);

        System.out.println("Fetching " + uri);
        RequestOptions options = new RequestOptions();
        if (lastUpdate != null) {
            options.setIfModifiedSince(lastUpdate);
        }

        long start = currentTimeMillis();
        HTTPResponse response = httpCache.execute(new HTTPRequest(uri, HTTPMethod.GET));
        long end = currentTimeMillis();
        System.out.println("Fetched in " + (end - start) + "ms");

        // Use the server's timestamp
        Date responseDate = response.getDate().toDate();

        System.out.println("responseDate = " + responseDate);

        Document<Element> document = null;
        try {
            document = parser.parse(response.getPayload().getInputStream());
        } catch (ParseException e) {
            System.out.println("Error parsing " + uri);
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

            if (lastUpdate != null && lastUpdate.after(published)) {
                System.out.println("Old entry: " + uri + ":" + entryId);
                continue;
            }

            System.out.println("New entry: " + uri + ":" + entryId);
            if (eventDao.countEntryId(entryId) == 0) {
                eventDao.insertChange(entryId, title);
            } else {
                System.out.println("Already imported entry: " + entryId);
            }
        }

        if (lastUpdate == null) {
            System.out.println("New atom feed");
            atomDao.insertAtomFeed(uri, new Timestamp(responseDate.getTime()));
        } else {
            System.out.println("Updating atom feed");
            atomDao.updateAtomFeed(uri, lastUpdate);
        }
    }
}

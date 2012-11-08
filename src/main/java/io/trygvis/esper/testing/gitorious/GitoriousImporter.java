package io.trygvis.esper.testing.gitorious;

import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.ResourceManager.*;
import org.apache.abdera.*;
import org.apache.abdera.model.*;
import org.apache.abdera.protocol.client.*;
import org.apache.abdera.protocol.client.cache.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.client.*;

import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.*;

public class GitoriousImporter {
    private final AbderaClient abderaClient;
    private final Connection c;
    private final AtomDao atomDao;
    private final GitoriousDao gitoriousDao;

    public static void main(String[] args) throws Exception {
        Main.configureLog4j();
        new GitoriousImporter();
    }

    public GitoriousImporter() throws Exception {
        Abdera abdera = new Abdera();
        abderaClient = new AbderaClient(abdera, new LRUCache(abdera, 1000));

        c = DriverManager.getConnection(DbMain.JDBC_URL, "esper", "");
        c.setAutoCommit(false);

        atomDao = new AtomDao(c);
        gitoriousDao = new GitoriousDao(c);

        HTTPCache httpCache = new HTTPCache(new MemoryCacheStorage(), HTTPClientResponseResolver.createMultithreadedInstance());

        final GitoriousClient gitoriousClient = new GitoriousClient(httpCache, "https://gitorious.org");

//        Set<GitoriousProject> projects = gitoriousClient.findProjects();
//
//        System.out.println("projects.size() = " + projects.size());
//        for (GitoriousProject project : projects) {
//            System.out.println("project.repositories = " + project.repositories);
//        }

//        new GitoriousImporter(abderaClient, c).work();

        final ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);

        int projectsUpdateInterval = 1000;
        final int projectUpdateInterval = 1000;

        ResourceManager<GitoriousProject, GitoriousProjectResourceManager> gitoriousProjects = new ResourceManager<>(service, 1000,

            new ResourceManagerCallbacks<GitoriousProject, GitoriousProjectResourceManager>() {
                public Set<GitoriousProject> discover() throws Exception {
                    return gitoriousClient.findProjects();
                }

                public GitoriousProjectResourceManager onNew(GitoriousProject key) {
                    return new GitoriousProjectResourceManager(service, projectUpdateInterval, key);
                }

                public void onGone(GitoriousProject key, GitoriousProjectResourceManager manager) {
                    System.out.println("Project gone.");
                    manager.close();
                }
            });
        ;
    }

    class GitoriousProjectResourceManager extends ResourceManager<GitoriousRepository, GitoriousRepository> {

        public GitoriousProjectResourceManager(ScheduledExecutorService executorService, int delay, GitoriousProject key) {
            super(executorService, delay, new ResourceManagerCallbacks<GitoriousRepository, GitoriousRepository>() {
                public Set<GitoriousRepository> discover() throws Exception {
                    key
                }

                public GitoriousRepository onNew(GitoriousRepository key) {
                    throw new RuntimeException("Not implemented");
                }

                public void onGone(GitoriousRepository key, GitoriousRepository value) {
                    throw new RuntimeException("Not implemented");
                }
            });
        }
    }

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
}

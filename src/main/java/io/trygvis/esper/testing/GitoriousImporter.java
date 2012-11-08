package io.trygvis.esper.testing;

import org.apache.abdera.*;
import org.apache.abdera.model.*;
import org.apache.abdera.protocol.client.*;
import org.apache.abdera.protocol.client.cache.*;

import java.sql.*;
import java.util.Date;

public class GitoriousImporter {
    private final AbderaClient abderaClient;
    private final Connection connection;
    private final AtomDao atomDao;
    private final GitoriousDao gitoriousDao;

    public GitoriousImporter(AbderaClient abderaClient, Connection c) throws SQLException {
        this.abderaClient = abderaClient;
        this.connection = c;
        atomDao = new AtomDao(c);
        gitoriousDao = new GitoriousDao(c);
    }

    public static void main(String[] args) throws InterruptedException, SQLException {
        Main.configureLog4j();
        Abdera abdera = new Abdera();
        AbderaClient abderaClient = new AbderaClient(abdera, new LRUCache(abdera, 1000));

        Connection connection = DriverManager.getConnection(DbMain.JDBC_URL, "esper", "");
        connection.setAutoCommit(false);

        new GitoriousImporter(abderaClient, connection).work();
    }

    private void work() throws SQLException, InterruptedException {
        String url = "http://qt.gitorious.org/projects/show/qt.atom";

        while (true) {
            Timestamp lastUpdate = atomDao.getAtomFeed(url);

            System.out.println("Fetching " + url);
            RequestOptions options = new RequestOptions();
            if(lastUpdate != null) {
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
                if(gitoriousDao.countEntryId(entryId) == 0) {
                    gitoriousDao.insertChange(entryId, title);
                }
                else {
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

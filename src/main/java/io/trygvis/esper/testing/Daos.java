package io.trygvis.esper.testing;

import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.gitorious.*;
import io.trygvis.esper.testing.jenkins.*;

import java.io.*;
import java.sql.*;

public class Daos implements Closeable {

    private final Connection connection;
    public final FileDao fileDao;
    public final GitoriousEventDao gitoriousEventDao;
    public final GitoriousProjectDao gitoriousProjectDao;
    public final GitoriousRepositoryDao gitoriousRepositoryDao;
    public final JenkinsDao jenkinsDao;
    public final PersonDao personDao;
    public final BuildDao buildDao;
    public final int seq;
    public static int counter = 1;

    public Daos(Connection c) throws SQLException {
        this.connection = c;
        this.seq = counter++;
        fileDao = new FileDao(c);
        gitoriousEventDao = new GitoriousEventDao(c);
        gitoriousProjectDao = new GitoriousProjectDao(c);
        gitoriousRepositoryDao = new GitoriousRepositoryDao(c);
        jenkinsDao = new JenkinsDao(c);
        personDao = new PersonDao(c);
        buildDao = new BuildDao(c);
    }

    public void close() throws IOException {
        System.out.println("Closing connection " + seq);
        try {
            connection.rollback();
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() throws SQLException {
        connection.rollback();
    }
}

package io.trygvis.esper.testing;

import io.trygvis.esper.testing.gitorious.*;

import javax.sql.*;
import java.io.*;
import java.sql.*;

public class Daos implements Closeable {
    private final Connection connection;
    public final GitoriousEventDao gitoriousEventDao;
    public final GitoriousProjectDao gitoriousProjectDao;
    public final GitoriousRepositoryDao gitoriousRepositoryDao;
    public final int seq;
    public static int counter = 1;

    public Daos(Connection connection) throws SQLException {
        this.connection = connection;
        this.seq = counter++;
        gitoriousEventDao = new GitoriousEventDao(connection);
        gitoriousProjectDao = new GitoriousProjectDao(connection);
        gitoriousRepositoryDao = new GitoriousRepositoryDao(connection);

        System.out.println("Opened connection " + seq);
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

    public static Daos lookup(DataSource dataSource) throws SQLException {
        return new Daos(dataSource.getConnection());
    }
}

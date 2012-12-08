package io.trygvis.esper.testing;

import io.trygvis.esper.testing.gitorious.*;

import javax.sql.*;
import java.io.*;
import java.sql.*;

public class Daos implements Closeable {

    public enum OrderDirection {
        ASC, DESC, NONE;

        public String toSql(String expression) {
           switch (this) {
               case ASC:
                   return expression + "expression";
               case DESC:
                   return expression + "expression DESC";
               default:
                   return "1";
           }
        }
    }

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
}

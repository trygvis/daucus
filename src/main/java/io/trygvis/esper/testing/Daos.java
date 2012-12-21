package io.trygvis.esper.testing;

import io.trygvis.esper.testing.core.*;
import io.trygvis.esper.testing.gitorious.*;
import io.trygvis.esper.testing.jenkins.*;

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
    public final JenkinsDao jenkinsDao;
    public final CoreDao coreDao;
    public final int seq;
    public static int counter = 1;

    public Daos(Connection c) throws SQLException {
        this.connection = c;
        this.seq = counter++;
        gitoriousEventDao = new GitoriousEventDao(c);
        gitoriousProjectDao = new GitoriousProjectDao(c);
        gitoriousRepositoryDao = new GitoriousRepositoryDao(c);
        jenkinsDao = new JenkinsDao(c);
        coreDao = new CoreDao(c);
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

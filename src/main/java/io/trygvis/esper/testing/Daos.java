package io.trygvis.esper.testing;

import io.trygvis.esper.testing.gitorious.*;

import java.sql.*;

public class Daos {
    public final AtomDao atomDao;
    public final GitoriousEventDao gitoriousEventDao;
    public final GitoriousProjectDao gitoriousProjectDao;
    public final GitoriousRepositoryDao gitoriousRepositoryDao;
    public final PreparedStatement begin;

    public Daos(Connection c) throws SQLException {
        atomDao = new AtomDao(c);
        gitoriousEventDao = new GitoriousEventDao(c);
        gitoriousProjectDao = new GitoriousProjectDao(c);
        gitoriousRepositoryDao = new GitoriousRepositoryDao(c);
        begin = c.prepareStatement("BEGIN");
    }

    public void begin() throws SQLException {
        begin.executeUpdate();
    }
}

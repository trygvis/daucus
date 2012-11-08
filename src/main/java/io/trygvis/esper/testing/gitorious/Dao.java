package io.trygvis.esper.testing.gitorious;

import java.sql.*;

public class Dao {
    private final Connection c;

    protected Dao(Connection c) {
        this.c = c;
    }

    protected PreparedStatement prepareStatement(String sql) throws SQLException {
        return c.prepareStatement(sql);
    }
}

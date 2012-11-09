package io.trygvis.esper.testing.gitorious;

import fj.*;

import java.sql.*;

public class Dao {
    private final Connection c;

    protected Dao(Connection c) {
        this.c = c;
    }

    protected PreparedStatement prepareStatement(String sql) throws SQLException {
        return c.prepareStatement(sql);
    }

    public static final F<Timestamp, java.util.Date> timestampToDate = new F<Timestamp, java.util.Date>() {
        public java.util.Date f(Timestamp timestamp) {
            return new java.util.Date(timestamp.getTime());
        }
    };

    public static final F<java.util.Date, Timestamp> dateToTimestamp = new F<java.util.Date, Timestamp>() {
        public Timestamp f(java.util.Date date) {
            return new Timestamp(date.getTime());
        }
    };
}

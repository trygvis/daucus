package io.trygvis.esper.testing;

import fj.*;
import org.joda.time.*;

import java.sql.*;
import java.util.*;

public class Dao {
    private final Connection c;
    private final Map<String, PreparedStatement> statements = new HashMap<>();

    protected Dao(Connection c) {
        this.c = c;
    }

    protected PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement s = statements.get(sql);

        if (s != null) {
            return s;
        }

        statements.put(sql, c.prepareStatement(sql));

        return s;
    }

    public static final F<Timestamp, java.util.Date> timestampToDate = new F<Timestamp, java.util.Date>() {
        public java.util.Date f(Timestamp timestamp) {
            return new java.util.Date(timestamp.getTime());
        }
    };

    public static final F<Timestamp, LocalDateTime> timestampToLocalDateTime = new F<Timestamp, LocalDateTime>() {
        public LocalDateTime f(Timestamp timestamp) {
            return new LocalDateTime(timestamp.getTime());
        }
    };

    public static final F<java.util.Date, Timestamp> dateToTimestamp = new F<java.util.Date, Timestamp>() {
        public Timestamp f(java.util.Date date) {
            return new Timestamp(date.getTime());
        }
    };
}

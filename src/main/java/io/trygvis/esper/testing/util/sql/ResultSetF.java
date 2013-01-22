package io.trygvis.esper.testing.util.sql;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.sql.*;
import java.util.*;

public class ResultSetF {
    public static final SqlF<ResultSet, Integer> getInt = new SqlF<ResultSet, Integer>() {
        public Integer apply(ResultSet rs) throws SQLException {
            return rs.getInt(1);
        }
    };

    public static final SqlF<ResultSet, Integer> getInteger = new SqlF<ResultSet, Integer>() {
        public Integer apply(ResultSet rs) throws SQLException {
            int i = rs.getInt(1);
            return rs.wasNull() ? null : i;
        }
    };

    public static final SqlF<ResultSet, DateTime> getDateTime = new SqlF<ResultSet, DateTime>() {
        public DateTime apply(ResultSet rs) throws SQLException {
            Timestamp i = rs.getTimestamp(1);
            return rs.wasNull() ? null : new DateTime(i.getTime());
        }
    };

    public static final SqlF<ResultSet, UUID> getUUID = new SqlF<ResultSet, UUID>() {
        public UUID apply(ResultSet rs) throws SQLException {
            return UUID.fromString(rs.getString(1));
        }
    };

    public static final SqlF<ResultSet, Uuid> getUuid = new SqlF<ResultSet, Uuid>() {
        public Uuid apply(ResultSet rs) throws SQLException {
            return Uuid.fromString(rs.getString(1));
        }
    };
}

package io.trygvis.esper.testing.util.sql;

import java.sql.*;

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
}

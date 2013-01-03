package io.trygvis.esper.testing.util.sql;

import java.sql.*;

public abstract class SqlF<A, B> {
    public abstract B apply(A a) throws SQLException;

    public <C> SqlF<A, C> andThen(final SqlF<B, C> f) {
        return new SqlF<A, C>() {
            public C apply(A a) throws SQLException {
                return f.apply(SqlF.this.apply(a));
            }
        };
    }
}

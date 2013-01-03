package io.trygvis.esper.testing.util.sql;

import java.sql.*;

public abstract class SqlP0<A> {
    public abstract A apply() throws SQLException;
}

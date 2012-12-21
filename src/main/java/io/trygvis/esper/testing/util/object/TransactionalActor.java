package io.trygvis.esper.testing.util.object;

import java.sql.*;

public interface TransactionalActor {
    void act(Connection c) throws Exception;
}

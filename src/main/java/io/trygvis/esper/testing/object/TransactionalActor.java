package io.trygvis.esper.testing.object;

import java.sql.*;

public interface TransactionalActor {
    void act(Connection c) throws Exception;
}

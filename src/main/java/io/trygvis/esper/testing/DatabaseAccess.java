package io.trygvis.esper.testing;

import org.slf4j.*;

import javax.sql.*;
import java.sql.*;

public class DatabaseAccess {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DataSource dataSource;

    public static interface DaosCallback<A> {
        A run(Daos daos) throws SQLException;
    }

    public DatabaseAccess(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <A> A inTransaction(DaosCallback<A> callback) throws SQLException {
        try {
            Connection c = dataSource.getConnection();

            try {
//                try (PreparedStatement s = c.prepareStatement("set application_name = 'Actor: " + name + "';")) {
//                    s.executeUpdate();
//                    s.close();
//                }

                Daos daos = new Daos(c);
                A a = callback.run(daos);
                daos.commit();

                return a;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.close();
            }
        } catch (Throwable e) {
            logger.warn("Exception in thread " + Thread.currentThread().getName());
//            e.printStackTrace(System.out);
            throw e;
        }
    }
}

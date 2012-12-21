package io.trygvis.esper.testing.core;

import fj.data.*;
import io.trygvis.esper.testing.sql.*;
import org.joda.time.format.*;
import org.slf4j.*;

import javax.sql.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import static fj.data.Option.*;
import static java.lang.System.*;

public class TablePoller<A> {

    private static final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String pollerName;
    private final String tableName;
    private final String columnNames;
    private final String filter;
    private final SqlF<ResultSet, A> f;
    private final NewRowCallback<A> callback;

    public TablePoller(String pollerName, String tableName, String columnNames, Option<String> filter, SqlF<ResultSet, A> f, NewRowCallback<A> callback) {
        this.pollerName = pollerName;
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.filter = filter.orSome("true");
        this.f = f;
        this.callback = callback;
    }

    public void work(DataSource dataSource) throws Exception {
        while (true) {
            try (Connection c = dataSource.getConnection()) {
                long start = currentTimeMillis();
                TablePollerDao dao = new TablePollerDao(c);

                Option<Timestamp> o = dao.getLastCreatedDateForPoller();

                if (o.isNone()) {
                    logger.info("First run of poller '{}'", pollerName);
                } else {
                    logger.info("Running poller '{}', last run was {}", pollerName, formatter.print(o.some().getTime()));
                }

                Timestamp lastCreatedDate = o.orSome(new Timestamp(0));

                Option<Timestamp> o2 = dao.getOldestCreatedDateAfter(lastCreatedDate);

                if (o2.isSome()) {
                    Timestamp oldestCreatedDate = o2.some();

                    List<A> rows = dao.getRowsCreatedAt(oldestCreatedDate);

                    logger.info("Processing {} rows created at {}", rows.size(), formatter.print(oldestCreatedDate.getTime()));

                    for (A row : rows) {
                        callback.process(c, row);
                    }
                } else {
                    logger.debug("No new rows.");

                    Thread.sleep(1000);
                }

                dao.insertOrUpdate(o.isNone(), o2.orSome(new Timestamp(start)), new Timestamp(start), currentTimeMillis() - start, null);

                c.commit();
            }
        }
    }

    public static interface NewRowCallback<A> {
        void process(Connection c, A A) throws SQLException;
    }

    private class TablePollerDao {
        private final Connection c;

        private TablePollerDao(Connection c) {
            this.c = c;
        }

        public Option<Timestamp> getLastCreatedDateForPoller() throws SQLException {
            try (PreparedStatement s = c.prepareStatement("SELECT last_created_date FROM table_poller_status WHERE poller_name=?")) {
                s.setString(1, pollerName);
                ResultSet rs = s.executeQuery();
                if (!rs.next()) {
                    return none();
                }

                return some(rs.getTimestamp(1));
            }
        }

        public Option<Timestamp> getOldestCreatedDateAfter(Timestamp timestamp) throws SQLException {
            try (PreparedStatement s = c.prepareStatement("SELECT min(created_date) FROM " + tableName + " WHERE created_date > ? AND " + filter)) {
                s.setTimestamp(1, timestamp);
                ResultSet rs = s.executeQuery();
                rs.next();
                return fromNull(rs.getTimestamp(1));
            }
        }

        public List<A> getRowsCreatedAt(Timestamp timestamp) throws SQLException {
            try (PreparedStatement s = c.prepareStatement("SELECT " + columnNames + " FROM " + tableName + " WHERE created_date = ? AND " + filter)) {
                s.setTimestamp(1, timestamp);

                ResultSet rs = s.executeQuery();

                List<A> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(f.apply(rs));
                }
                return list;
            }
        }

        public void insertOrUpdate(boolean insert, Timestamp lastCreatedDate, Timestamp now, long duration, String status) throws SQLException {
            String insertSql = "INSERT INTO table_poller_status(last_created_date, last_run, duration, status, poller_name) VALUES(?, ?, ?, ?, ?)";

            String updateSql = "UPDATE table_poller_status SET last_created_date=?, last_run=?, duration=?, status=? WHERE poller_name=?";

            try (PreparedStatement s = c.prepareStatement(insert ? insertSql : updateSql)) {
                int i = 1;
                s.setTimestamp(i++, lastCreatedDate);
                s.setTimestamp(i++, now);
                s.setLong(i++, duration);
                s.setString(i++, status);
                s.setString(i, pollerName);
                s.executeUpdate();
            }
        }
    }
}

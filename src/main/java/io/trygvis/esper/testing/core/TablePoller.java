package io.trygvis.esper.testing.core;

import fj.data.*;
import io.trygvis.esper.testing.sql.*;
import org.joda.time.format.*;
import org.slf4j.*;

import javax.sql.*;
import java.sql.*;

import static io.trygvis.esper.testing.sql.ResultSetF.*;
import static io.trygvis.esper.testing.sql.SqlOption.*;
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

                SqlOption<Integer> o = dao.getLastSequenceForPoller();

                if (o.isNone()) {
                    logger.info("First run of poller '{}'", pollerName);
                } else {
                    logger.info("Running poller '{}', last seq was {}", pollerName, o.get());
                }

                int seq = o.getOrElse(0);
                int count = 0;

                Integer seqO = dao.getMinSeqAfter(seq);

                while (seqO != null) {
                    seq = seqO;

                    logger.info("Processing seq={}", seq);

                    A row = dao.getRow(seq);

                    callback.process(c, row);

                    seqO = dao.getMinSeqAfter(seq);
                    count++;
                }

                if(count > 0) {
                    logger.info("Processed {} rows.", count);
                }
                else {
                    logger.debug("No new rows.");
                }

                Thread.sleep(10 * 1000);

                dao.insertOrUpdate(o.isNone(), seq, new Timestamp(start), currentTimeMillis() - start, null);

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

        public SqlOption<Integer> getLastSequenceForPoller() throws SQLException {
            try (PreparedStatement s = c.prepareStatement("SELECT last_seq FROM table_poller_status WHERE poller_name=?")) {
                s.setString(1, pollerName);
                return fromRs(s.executeQuery()).map(getInt);
            }
        }

        public Integer getMinSeqAfter(int seq) throws SQLException {
            try (PreparedStatement s = c.prepareStatement("SELECT min(seq) FROM " + tableName + " WHERE seq>? AND " + filter)) {
                s.setInt(1, seq);
                return fromRs(s.executeQuery()).map(getInteger).get();
            }
        }

        public A getRow(int seq) throws SQLException {
            try (PreparedStatement s = c.prepareStatement("SELECT " + columnNames + " FROM " + tableName + " WHERE seq=?")) {
                s.setInt(1, seq);
                return fromRs(s.executeQuery()).map(f).get();
            }
        }

        public void insertOrUpdate(boolean insert, int seq, Timestamp now, long duration, String status) throws SQLException {
            String insertSql = "INSERT INTO table_poller_status(last_seq, last_run, duration, status, poller_name) VALUES(?, ?, ?, ?, ?)";

            String updateSql = "UPDATE table_poller_status SET last_seq=?, last_run=?, duration=?, status=? WHERE poller_name=?";

            try (PreparedStatement s = c.prepareStatement(insert ? insertSql : updateSql)) {
                int i = 1;
                s.setInt(i++, seq);
                s.setTimestamp(i++, now);
                s.setLong(i++, duration);
                s.setString(i++, status);
                s.setString(i, pollerName);
                s.executeUpdate();
            }
        }
    }
}

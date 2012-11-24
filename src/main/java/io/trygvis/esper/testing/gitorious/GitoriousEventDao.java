package io.trygvis.esper.testing.gitorious;

import static io.trygvis.esper.testing.DaoUtil.*;

import java.sql.*;

public class GitoriousEventDao {

    protected final Connection c;

    public GitoriousEventDao(Connection c) {
        this.c = c;
    }

    public int countEntryId(String entryId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT count(entry_id) FROM gitorious_event WHERE entry_id=?")) {
            s.setString(1, entryId);
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void insertEvent(GitoriousEvent event) throws SQLException {
        PreparedStatement s = null;
        try {
            if (event instanceof GitoriousPush) {
                GitoriousPush push = (GitoriousPush) event;
                s = c.prepareStatement("INSERT INTO gitorious_event(project_slug, name, entry_id, published, title, content, event_type, who, \"from\", \"to\", branch, commit_count) VALUES(?, ?, ?, ?, ?, ?, 'PUSH', ?, ?, ?, ?, ?)");
                s.setString(7, push.who);
                s.setString(8, push.from);
                s.setString(9, push.to);
                s.setString(10, push.branch);
                s.setInt(11, push.commitCount);
            } else {
                throw new SQLException("Unknown event type: " + event.getClass().getName());
            }

            s.setString(1, event.projectSlug);
            s.setString(2, event.name);
            s.setString(3, event.entryId);
            s.setTimestamp(4, dateToTimestamp.f(event.published));
            s.setString(5, event.title);
            s.setString(6, event.content);
            s.executeUpdate();
        } finally {
            if(s != null) {
                s.close();
            }
        }
    }
}

package io.trygvis.esper.testing.gitorious;

import java.sql.*;

public class GitoriousEventDao extends Dao {

    public GitoriousEventDao(Connection c) throws SQLException {
        super(c);
    }

    private final PreparedStatement countEntryId = prepareStatement("SELECT count(entry_id) FROM gitorious_event WHERE entry_id=?");

    public int countEntryId(String entryId) throws SQLException {
        countEntryId.setString(1, entryId);
        try (ResultSet rs = countEntryId.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private final PreparedStatement insertPush = prepareStatement("INSERT INTO gitorious_event(project_slug, name, entry_id, published, title, content, event_type, who, \"from\", \"to\", branch, commit_count) VALUES(?, ?, ?, ?, ?, ?, 'PUSH', ?, ?, ?, ?, ?)");

    public void insertEvent(GitoriousEvent event) throws SQLException {
        PreparedStatement s;
        if (event instanceof GitoriousPush) {
            GitoriousPush push = (GitoriousPush) event;
            s = insertPush;
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
    }
}

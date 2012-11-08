package io.trygvis.esper.testing;

import java.sql.*;

public class GitoriousDao {
    private final PreparedStatement countEntryId;
    private final PreparedStatement insertChange;

    public GitoriousDao(Connection c) throws SQLException {
        countEntryId = c.prepareStatement("SELECT count(entry_id) FROM gitorious_change WHERE entry_id=?");
        insertChange = c.prepareStatement("INSERT INTO gitorious_change(entry_id, text) VALUES(?, ?)");
    }

    public int countEntryId(String entryId) throws SQLException {
        countEntryId.setString(1, entryId);
        try(ResultSet rs = countEntryId.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public void insertChange(String entryId, String text) throws SQLException {
        insertChange.setString(1, entryId);
        insertChange.setString(2, text);
        insertChange.executeUpdate();
    }
}

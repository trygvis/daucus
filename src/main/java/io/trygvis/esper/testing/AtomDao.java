package io.trygvis.esper.testing;

import java.sql.*;

public class AtomDao {
    private final PreparedStatement selectLastUpdate;
    private final PreparedStatement insertAtomFeed;
    private final PreparedStatement updateAtomFeed;

    public AtomDao(Connection c) throws SQLException {
        selectLastUpdate = c.prepareStatement("SELECT last_update FROM atom_feed WHERE url=?");
        insertAtomFeed = c.prepareStatement("INSERT INTO atom_feed(url, last_update) VALUES(?, ?)");
        updateAtomFeed = c.prepareStatement("UPDATE atom_feed SET last_update=? WHERE url=?");
    }

    public Timestamp getAtomFeed(String url) throws SQLException {
        selectLastUpdate.setString(1, url);
        ResultSet rs = selectLastUpdate.executeQuery();
        if (!rs.next()) {
            return null;
        }

        return rs.getTimestamp(1);
    }

    public void insertAtomFeed(String url, Timestamp lastUpdate) throws SQLException {
        insertAtomFeed.setString(1, url);
        insertAtomFeed.setTimestamp(2, lastUpdate);
        insertAtomFeed.executeUpdate();
    }

    public void updateAtomFeed(String url, Timestamp lastUpdate) throws SQLException {
        updateAtomFeed.setTimestamp(1, lastUpdate);
        updateAtomFeed.setString(2, url);
        updateAtomFeed.executeUpdate();
    }
}

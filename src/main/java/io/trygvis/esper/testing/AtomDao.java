package io.trygvis.esper.testing;

import java.net.*;
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

    public Timestamp getAtomFeed(URI uri) throws SQLException {
        selectLastUpdate.setString(1, uri.toASCIIString());
        ResultSet rs = selectLastUpdate.executeQuery();
        if (!rs.next()) {
            return null;
        }

        return rs.getTimestamp(1);
    }

    public void insertAtomFeed(URI uri, Timestamp lastUpdate) throws SQLException {
        insertAtomFeed.setString(1, uri.toASCIIString());
        insertAtomFeed.setTimestamp(2, lastUpdate);
        insertAtomFeed.executeUpdate();
    }

    public void updateAtomFeed(URI uri, Timestamp lastUpdate) throws SQLException {
        updateAtomFeed.setTimestamp(1, lastUpdate);
        updateAtomFeed.setString(2, uri.toASCIIString());
        updateAtomFeed.executeUpdate();
    }
}

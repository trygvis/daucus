package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.util.sql.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import static io.trygvis.esper.testing.util.sql.SqlOption.*;
import static java.lang.System.*;

public class FileDao {
    private final Connection c;

    public FileDao(Connection c) {
        this.c = c;
    }

    public UUID store(URI url, String contentType, byte[] data) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO file(uuid, created_date, url, content_type, data) VALUES(?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, url.toASCIIString());
            s.setString(i++, contentType);
            s.setBinaryStream(i, new ByteArrayInputStream(data), data.length);
            s.executeUpdate();
            return uuid;
        }
    }

    public SqlOption<InputStream> load(UUID uuid) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT data FROM file WHERE uuid=?")) {
            s.setString(1, uuid.toString());
            ResultSet rs = s.executeQuery();
            if(!rs.next()) {
                return none();
            }
            return some(rs.getBinaryStream(1));
        }
    }

    public SqlOption<InputStream> loadByUrl(URI uri) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT data FROM file WHERE url=?")) {
            s.setString(1, uri.toASCIIString());
            ResultSet rs = s.executeQuery();
            if(!rs.next()) {
                return none();
            }
            return some(rs.getBinaryStream(1));
        }
    }
}

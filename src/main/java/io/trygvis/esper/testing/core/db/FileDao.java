package io.trygvis.esper.testing.core.db;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import static java.lang.System.*;

public class FileDao {
    private final Connection c;

    public FileDao(Connection c) {
        this.c = c;
    }

    public void store(URI url, String contentType, byte[] data) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO file(uuid, created_date, url, content_type, data) VALUES(?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, url.toASCIIString());
            s.setString(i++, contentType);
            s.setBinaryStream(i, new ByteArrayInputStream(data), data.length);
            s.executeUpdate();
        }
    }
}

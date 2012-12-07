package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import org.joda.time.*;

import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static fj.data.Option.none;
import static fj.data.Option.some;
import static java.lang.System.currentTimeMillis;

public class JenkinsDao {

    private final Connection c;

    private static final String JENKINS_SERVER = "uuid, created_date, url";

    private static final String JENKINS_BUILD = "uuid, created_date, entry_id, url, result, number, duration, timestamp";

    public JenkinsDao(Connection c) {
        this.c = c;
    }

    private JenkinsServerDto jenkinsServer(ResultSet rs) throws SQLException {
        int i = 1;
        return new JenkinsServerDto(
                UUID.fromString(rs.getString(i++)),
                new DateTime(rs.getTimestamp(i++).getTime()),
                URI.create(rs.getString(i)));
    }

//    private JenkinsEventDto jenkinsEvent(ResultSet rs) throws SQLException {
//        int i = 1;
//        return new JenkinsEventDto(
//                UUID.fromString(rs.getString(i++)),
//                new DateTime(rs.getTimestamp(i++).getTime()),
//                URI.create(rs.getString(i)));
//    }

    private JenkinsBuildDto jenkinsBuild(ResultSet rs) throws SQLException {
        int i = 1;
        return new JenkinsBuildDto(
                UUID.fromString(rs.getString(i++)),
                new DateTime(rs.getTimestamp(i++).getTime()),
                rs.getString(i++),
                URI.create(rs.getString(i++)),
                rs.getString(i++),
                rs.getInt(i++),
                rs.getInt(i++),
                new DateTime(rs.getTimestamp(i).getTime()));
    }

    public List<JenkinsServerDto> selectServer() throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + JENKINS_SERVER + " FROM jenkins_server")) {
            ResultSet rs = s.executeQuery();

            List<JenkinsServerDto> servers = new ArrayList<>();
            while (rs.next()) {
                servers.add(jenkinsServer(rs));
            }
            return servers;
        }
    }

    public Option<JenkinsBuildDto> selectBuildByEntryId(String id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + JENKINS_BUILD + " FROM jenkins_build WHERE entry_id=?")) {
            int i = 1;
            s.setString(i, id);
            ResultSet rs = s.executeQuery();

            if (!rs.next()) {
                return none();
            }

            return some(jenkinsBuild(rs));
        }
    }

    public UUID insertBuild(String entryId, URI uri, String result, int number, int duration, long timestamp) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO jenkins_build(" + JENKINS_BUILD + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, entryId);
            s.setString(i++, uri.toASCIIString());
            s.setString(i++, result);
            s.setInt(i++, number);
            s.setInt(i++, duration);
            s.setTimestamp(i, new Timestamp(timestamp));
            s.executeUpdate();

            return uuid;
        }
    }
}

class JenkinsServerDto {
    public final UUID uuid;
    public final DateTime created_date;
    public final URI uri;

    JenkinsServerDto(UUID uuid, DateTime created_date, URI uri) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JenkinsServerDto that = (JenkinsServerDto) o;

        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}

class JenkinsJobDto {
    public final UUID uuid;
    public final DateTime created_date;
    public final URI uri;
    public final String title;

    JenkinsJobDto(UUID uuid, DateTime created_date, URI uri, String title) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.uri = uri;
        this.title = title;
    }
}

class JenkinsBuildDto {
    public final UUID uuid;
    public final DateTime created_date;
    public final String entryId;
    public final URI uri;
    public final String result;
    public final int number;
    public final int duration;
    public final DateTime timestamp;

    JenkinsBuildDto(UUID uuid, DateTime created_date, String entryId, URI uri, String result, int number, int duration, DateTime timestamp) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.entryId = entryId;
        this.uri = uri;
        this.result = result;
        this.number = number;
        this.duration = duration;
        this.timestamp = timestamp;
    }
}

class JenkinsEventDto {
    public final UUID uuid;
    public final DateTime created_date;
    public final String id;
    public final DateTime timestamp;

    JenkinsEventDto(UUID uuid, DateTime created_date, String id, DateTime timestamp) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.id = id;
        this.timestamp = timestamp;
    }
}

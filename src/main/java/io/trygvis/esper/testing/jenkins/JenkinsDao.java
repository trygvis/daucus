package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import org.joda.time.*;

import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static fj.data.Option.fromNull;
import static fj.data.Option.none;
import static fj.data.Option.some;
import static java.lang.System.currentTimeMillis;

public class JenkinsDao {

    private final Connection c;

    public static final String JENKINS_SERVER = "uuid, created_date, url";

    public static final String JENKINS_JOB = "uuid, created_date, server, url, job_type, display_name";

    public static final String JENKINS_BUILD = "uuid, created_date, job, entry_id, url, result, number, duration, timestamp";

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

    public List<JenkinsServerDto> toServerList(ResultSet rs) throws SQLException {
        List<JenkinsServerDto> list = new ArrayList<>();
        while (rs.next()) {
            list.add(jenkinsServer(rs));
        }
        return list;
    }

    private JenkinsJobDto jenkinsJob(ResultSet rs) throws SQLException {
        int i = 1;
        return new JenkinsJobDto(
                UUID.fromString(rs.getString(i++)),
                new DateTime(rs.getTimestamp(i++).getTime()),
                UUID.fromString(rs.getString(i++)),
                URI.create(rs.getString(i++)),
                fromNull(rs.getString(i)));
    }

    public List<JenkinsJobDto> toJobList(ResultSet rs) throws SQLException {
        List<JenkinsJobDto> list = new ArrayList<>();
        while (rs.next()) {
            list.add(jenkinsJob(rs));
        }
        return list;
    }

    private JenkinsBuildDto jenkinsBuild(ResultSet rs) throws SQLException {
        int i = 1;
        return new JenkinsBuildDto(
                UUID.fromString(rs.getString(i++)),
                new DateTime(rs.getTimestamp(i++).getTime()),
                UUID.fromString(rs.getString(i++)),
                rs.getString(i++),
                URI.create(rs.getString(i++)),
                rs.getString(i++),
                rs.getInt(i++),
                rs.getInt(i++),
                new DateTime(rs.getTimestamp(i).getTime()));
    }

    public List<JenkinsServerDto> selectServer(boolean enabledOnly) throws SQLException {
        String where = "WHERE ";
        where += enabledOnly ? "enabled=true" : "";

        try (PreparedStatement s = c.prepareStatement("SELECT " + JENKINS_SERVER + " FROM jenkins_server " + where)) {
            return toServerList(s.executeQuery());
        }
    }

    public Option<JenkinsJobDto> selectJobByUrl(URI url) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + JENKINS_JOB + " FROM jenkins_job WHERE url=?")) {
            s.setString(1, url.toASCIIString());
            ResultSet rs = s.executeQuery();

            if(!rs.next()) {
                return none();
            }

            return some(jenkinsJob(rs));
        }
    }

    public UUID insertJob(UUID server, URI url, JenkinsJobXml.JenkinsJobType type, Option<String> displayName) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO jenkins_job(" + JENKINS_JOB + ") VALUES(?, ?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, server.toString());
            s.setString(i++, url.toASCIIString());
            s.setString(i++, type.name());
            s.setString(i, displayName.toNull());
            s.executeUpdate();

            return uuid;
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

    public UUID insertBuild(UUID server, String entryId, URI url, String result, int number, int duration, long timestamp) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO jenkins_build(" + JENKINS_BUILD + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, server.toString());
            s.setString(i++, entryId);
            s.setString(i++, url.toASCIIString());
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
    public final URI url;

    JenkinsServerDto(UUID uuid, DateTime created_date, URI url) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.url = url;
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
    public final UUID server;
    public final URI url;
    public final Option<String> displayName;

    JenkinsJobDto(UUID uuid, DateTime created_date, UUID server, URI url, Option<String> displayName) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.server = server;
        this.url = url;
        this.displayName = displayName;
    }
}

class JenkinsBuildDto {
    public final UUID uuid;
    public final DateTime created_date;
    public final UUID job;
    public final String entryId;
    public final URI url;
    public final String result;
    public final int number;
    public final int duration;
    public final DateTime timestamp;

    JenkinsBuildDto(UUID uuid, DateTime created_date, UUID job, String entryId, URI url, String result, int number, int duration, DateTime timestamp) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.job = job;
        this.entryId = entryId;
        this.url = url;
        this.result = result;
        this.number = number;
        this.duration = duration;
        this.timestamp = timestamp;
    }
}

package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import io.trygvis.esper.testing.sql.*;
import org.joda.time.*;

import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import static fj.data.Option.*;
import static io.trygvis.esper.testing.sql.SqlOption.fromRs;
import static java.lang.System.*;

public class JenkinsDao {

    private final Connection c;

    public static final String JENKINS_SERVER = "uuid, created_date, url, enabled";

    public static final String JENKINS_JOB = "uuid, created_date, server, url, job_type, display_name";

    public static final String JENKINS_BUILD = "uuid, created_date, job, entry_id, url, result, number, duration, timestamp";

    public static final String JENKINS_USER = "uuid, created_date, server, absolute_url";

    public JenkinsDao(Connection c) {
        this.c = c;
    }

    private JenkinsServerDto jenkinsServer(ResultSet rs) throws SQLException {
        int i = 1;
        return new JenkinsServerDto(
                UUID.fromString(rs.getString(i++)),
                new DateTime(rs.getTimestamp(i++).getTime()),
                URI.create(rs.getString(i++)),
                rs.getBoolean(i));
    }

    private List<JenkinsServerDto> toServerList(ResultSet rs) throws SQLException {
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

    public static final SqlF<ResultSet, JenkinsBuildDto> jenkinsBuild = new SqlF<ResultSet, JenkinsBuildDto>() {
        public JenkinsBuildDto apply(ResultSet rs) throws SQLException {
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
    };

    public static final SqlF<ResultSet, JenkinsUserDto> jenkinsUser = new SqlF<ResultSet, JenkinsUserDto>() {
        public JenkinsUserDto apply(ResultSet rs) throws SQLException {
            int i = 1;
            return new JenkinsUserDto(
                    UUID.fromString(rs.getString(i++)),
                    new DateTime(rs.getTimestamp(i++).getTime()),
                    UUID.fromString(rs.getString(i++)),
                    rs.getString(i));
        }
    };

    public List<JenkinsServerDto> selectServers(boolean enabledOnly) throws SQLException {
        String sql = "SELECT " + JENKINS_SERVER + " FROM jenkins_server";

        if (enabledOnly) {
            sql += " WHERE enabled=true";
        }

        sql += " ORDER BY url";

        try (PreparedStatement s = c.prepareStatement(sql)) {
            return toServerList(s.executeQuery());
        }
    }

    public Option<JenkinsServerDto> selectServer(UUID uuid) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + JENKINS_SERVER + " FROM jenkins_server WHERE uuid=?")) {
            s.setString(1, uuid.toString());
            ResultSet rs = s.executeQuery();
            if (!rs.next()) {
                return none();
            }
            return some(jenkinsServer(rs));
        }
    }

    public Option<JenkinsJobDto> selectJobByUrl(URI url) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + JENKINS_JOB + " FROM jenkins_job WHERE url=?")) {
            s.setString(1, url.toASCIIString());
            ResultSet rs = s.executeQuery();

            if (!rs.next()) {
                return none();
            }

            return some(jenkinsJob(rs));
        }
    }

    public int selectJobCountForServer(UUID uuid) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT count(1) FROM jenkins_job WHERE server=?")) {
            s.setString(1, uuid.toString());
            ResultSet rs = s.executeQuery();
            rs.next();
            return rs.getInt(1);
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

    public SqlOption<JenkinsBuildDto> selectBuildByEntryId(String id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + JENKINS_BUILD + " FROM jenkins_build WHERE entry_id=?")) {
            int i = 1;
            s.setString(i, id);
            return fromRs(s.executeQuery()).map(jenkinsBuild);
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

    public UUID insertUser(UUID server, String absoluteUrl) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO jenkins_user(" + JENKINS_USER + ") VALUES(?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, server.toString());
            s.setString(i, absoluteUrl);
            s.executeUpdate();

            return uuid;
        }
    }

    public SqlOption<JenkinsUserDto> selectUser(UUID server, String absoluteUrl) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + JENKINS_USER + " FROM jenkins_user WHERE server=? AND absolute_url=?")) {
            int i = 1;
            s.setString(i++, server.toString());
            s.setString(i, absoluteUrl);
            return fromRs(s.executeQuery()).map(jenkinsUser);
        }
    }
}

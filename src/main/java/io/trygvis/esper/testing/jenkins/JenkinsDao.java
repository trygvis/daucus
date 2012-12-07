package io.trygvis.esper.testing.jenkins;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JenkinsDao {

    private final Connection c;
    private static final String JENKINS_SERVER = "uuid, url";

    public JenkinsDao(Connection c) {
        this.c = c;
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

    private JenkinsServerDto jenkinsServer(ResultSet rs) throws SQLException {
        int i = 1;
        return new JenkinsServerDto(UUID.fromString(rs.getString(i++)), URI.create(rs.getString(i)));
    }
}

class JenkinsServerDto {
    public final UUID uuid;
    public final URI uri;

    JenkinsServerDto(UUID uuid, URI uri) {
        this.uuid = uuid;
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

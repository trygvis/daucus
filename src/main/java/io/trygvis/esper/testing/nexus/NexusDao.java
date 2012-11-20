package io.trygvis.esper.testing.nexus;

import fj.data.*;
import static fj.data.Option.*;
import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.net.*;
import java.sql.Array;
import java.sql.*;
import java.util.*;
import java.util.List;

public class NexusDao extends Dao {
    protected NexusDao(Connection c) {
        super(c);
    }

    /*
    public void insertRepository(String repositoryId, URI nexusUri, LocalDateTime discoveryDate) throws SQLException {
        int i = 1;
        try (PreparedStatement s = prepareStatement("INSERT INTO nexus_repository(id, uri, discovered_date) VALUES(?, ?, ?)")) {
            s.setString(i++, repositoryId);
            s.setString(i++, nexusUri.toASCIIString());
            s.setTimestamp(i, new Timestamp(discoveryDate.toDateTime().getMillis()));
            s.executeUpdate();
        }
    }
    */

    public List<NexusServerDto> selectServer() throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT url FROM nexus_server")) {
            ResultSet rs = s.executeQuery();

            List<NexusServerDto> servers = new ArrayList<>();
            while(rs.next()) {
                servers.add(new NexusServerDto(uri(rs.getString(1))));
            }
            return servers;
        }
    }

    public Option<NexusRepositoryDto> selectRepository(String repositoryId) throws SQLException {
        PreparedStatement s = prepareStatement("SELECT id, nexus_server_url, group_ids, discovery_date, last_update, last_successful_update FROM nexus_repository WHERE id=?");
        s.setString(1, repositoryId);

        try (ResultSet rs = s.executeQuery()) {
            if (!rs.next()) {
                return Option.none();
            }

            return some(toRepository(rs));
        }
    }

    public List<NexusRepositoryDto> findRepositories(URI nexusUrl) throws SQLException {
        PreparedStatement s = prepareStatement("SELECT id, nexus_server_url, group_ids, created_date, last_update, last_successful_update FROM nexus_repository WHERE nexus_server_url=?");
        s.setString(1, nexusUrl.toASCIIString());

        List<NexusRepositoryDto> list = new ArrayList<>();
        try (ResultSet rs = s.executeQuery()) {
            while(rs.next()) {
                list.add(toRepository(rs));
            }
        }
        return list;
    }

    private NexusRepositoryDto toRepository(ResultSet rs) throws SQLException {
        int i = 1;

        return new NexusRepositoryDto(
            rs.getString(i++),
            uri(rs.getString(i++)),
            (String[]) rs.getArray(i++).getArray(),
            fromNull(rs.getTimestamp(i++)).map(timestampToLocalDateTime),
            fromNull(rs.getTimestamp(i++)).map(timestampToLocalDateTime),
            fromNull(rs.getTimestamp(i)).map(timestampToLocalDateTime)
        );
    }

    private URI uri(String s) {
        try {
            return URI.create(s);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }
    }
}

class NexusServerDto {
    public final URI url;

    NexusServerDto(URI url) {
        this.url = url;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NexusServerDto)) return false;

        NexusServerDto that = (NexusServerDto) o;

        if (!url.equals(that.url)) return false;

        return true;
    }

    public int hashCode() {
        return url.hashCode();
    }
}

class NexusRepositoryDto {
    public final String repositoryId;
    public final URI nexusUrl;
    public final String[] groupIds;
    public final Option<LocalDateTime> discoveryDate;
    public final Option<LocalDateTime> lastUpdate;
    public final Option<LocalDateTime> lastSuccessfulUpdate;

    NexusRepositoryDto(String repositoryId, URI nexusUrl, String[] groupIds, Option<LocalDateTime> discoveryDate, Option<LocalDateTime> lastUpdate, Option<LocalDateTime> lastSuccessfulUpdate) {
        this.repositoryId = repositoryId;
        this.nexusUrl = nexusUrl;
        this.groupIds = groupIds;
        this.discoveryDate = discoveryDate;
        this.lastUpdate = lastUpdate;
        this.lastSuccessfulUpdate = lastSuccessfulUpdate;
    }
}

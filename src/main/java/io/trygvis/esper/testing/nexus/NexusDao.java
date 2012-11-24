package io.trygvis.esper.testing.nexus;

import fj.data.*;
import static fj.data.Option.*;
import io.trygvis.esper.testing.*;
import static io.trygvis.esper.testing.DaoUtil.timestampToLocalDateTime;
import org.joda.time.*;

import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;

public class NexusDao {
    private final Connection c;

    public NexusDao(Connection c) {
        this.c = c;
    }

    public static URI uri(String s) {
        try {
            return URI.create(s);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }
    }

    private NexusRepositoryDto nexusRepositoryDto(ResultSet rs) throws SQLException {
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
        try (PreparedStatement s = c.prepareStatement("SELECT url, name FROM nexus_server")) {
            ResultSet rs = s.executeQuery();

            List<NexusServerDto> servers = new ArrayList<>();
            while (rs.next()) {
                servers.add(new NexusServerDto(uri(rs.getString(1)), rs.getString(2)));
            }
            return servers;
        }
    }

    public Option<NexusRepositoryDto> findRepository(String repositoryId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT id, server_url, group_ids, discovery_date, last_update, last_successful_update FROM nexus_repository WHERE id=?")) {
            s.setString(1, repositoryId);

            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) {
                    return Option.none();
                }

                return some(nexusRepositoryDto(rs));
            }
        }
    }

    public List<NexusRepositoryDto> findRepositories(URI nexusUrl) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT id, server_url, group_ids, created_date, last_update, last_successful_update FROM nexus_repository WHERE server_url=?")) {
            s.setString(1, nexusUrl.toASCIIString());

            List<NexusRepositoryDto> list = new ArrayList<>();
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    list.add(nexusRepositoryDto(rs));
                }
            }

            return list;
        }
    }

    // -----------------------------------------------------------------------
    // Nexus Artifact
    // -----------------------------------------------------------------------

    private final String NEXUS_ARTIFACT_ID = "group_id, artifact_id, version";

    private final String NEXUS_ARTIFACT = NEXUS_ARTIFACT_ID + ", snapshot_version, classifiers, packagings, created_date";

    private int setArtifactId(int i, PreparedStatement s, ArtifactId id) throws SQLException {
        s.setString(i++, id.groupId);
        s.setString(i++, id.artifactId);
        s.setString(i++, id.version);
        return i;
    }

    private static ArtifactDto artifactDto(URI serverUrl, String repositoryId, ResultSet rs) throws SQLException {
        int i = 1;

        return new ArtifactDto(
            serverUrl,
            repositoryId,
            new ArtifactId(rs.getString(i++),
                rs.getString(i++),
                rs.getString(i)));
    }

    public void insertArtifact(URI nexusUrl, String repositoryId, ArtifactId id, Option<String> snapshotVersion, List<ArtifactFile> files, Date createdDate) throws SQLException {
        String[] classifiers = new String[files.size()];
        String[] packagings = new String[files.size()];

        for (int i = 0; i < files.size(); i++) {
            classifiers[i] = files.get(i).classifier.toNull();
            packagings[i] = files.get(i).extension;
        }

        int i = 1;
        try (PreparedStatement s = c.prepareStatement("INSERT INTO nexus_artifact(server_url, repository_id, " + NEXUS_ARTIFACT + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            s.setString(i++, nexusUrl.toASCIIString());
            s.setString(i++, repositoryId);
            i = setArtifactId(i, s, id);
            s.setString(i++, snapshotVersion.toNull());
            s.setArray(i++, c.createArrayOf("varchar", classifiers));
            s.setArray(i++, c.createArrayOf("varchar", packagings));
            s.setTimestamp(i, DaoUtil.dateToTimestamp.f(createdDate));
            s.executeUpdate();
        }
    }

    public void deleteArtifact(URI nexusUrl, String repositoryId, ArtifactId id) throws SQLException {
        int i = 1;
        try (PreparedStatement s = c.prepareStatement("DELETE FROM nexus_artifact WHERE server_url=? AND repository_id=? AND group_id=? AND artifact_id=? AND version=?")) {
            s.setString(i++, nexusUrl.toASCIIString());
            s.setString(i++, repositoryId);
            i += setArtifactId(i, s, id);
            s.executeUpdate();
        }
    }

    public List<ArtifactDto> findArtifactsInRepository(URI url, String repositoryId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + NEXUS_ARTIFACT + " FROM nexus_artifact WHERE server_url=? AND repository_id=?")) {
            s.setString(1, url.toASCIIString());
            s.setString(2, repositoryId);
            ResultSet rs = s.executeQuery();

            List<ArtifactDto> list = new ArrayList<>();
            while (rs.next()) {
                list.add(artifactDto(url, repositoryId, rs));
            }
            return list;
        }
    }
}

class NexusServerDto {
    public final URI url;
    public final String name;

    NexusServerDto(URI url, String name) {
        this.url = url;
        this.name = name;
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


package io.trygvis.esper.testing.nexus;

import fj.data.*;
import static fj.data.Option.*;
import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.sql.*;

public class NexusDao extends Dao {
    protected NexusDao(Connection c) {
        super(c);
    }

    public void insertRepository(String repositoryId, LocalDateTime discoveryDate) throws SQLException {
        PreparedStatement s = prepareStatement("INSERT INTO nexus_repository(id) VALUES(?)");
        s.setString(1, repositoryId);
        s.executeUpdate();
    }

    public Option<NexusRepository> selectRepository(String repositoryId) throws SQLException {
        PreparedStatement s = prepareStatement("SELECT id, discovery_date, last_update, last_successful_update FROM nexus_repository WHERE id=?");
        s.setString(1, repositoryId);

        try (ResultSet rs = s.executeQuery()) {
            if (!rs.next()) {
                return Option.none();
            }

            return some(new NexusRepository(
                    rs.getString(1),
                    fromNull(rs.getTimestamp(2)).map(timestampToLocalDateTime),
                    fromNull(rs.getTimestamp(3)).map(timestampToLocalDateTime),
                    fromNull(rs.getTimestamp(4)).map(timestampToLocalDateTime)
            ));
        }
    }
}

class NexusRepository {
    public final String repositoryId;
    public final Option<LocalDateTime> discoveryDate;
    public final Option<LocalDateTime> lastUpdate;
    public final Option<LocalDateTime> lastSuccessfulUpdate;

    NexusRepository(String repositoryId, Option<LocalDateTime> discoveryDate, Option<LocalDateTime> lastUpdate, Option<LocalDateTime> lastSuccessfulUpdate) {
        this.repositoryId = repositoryId;
        this.discoveryDate = discoveryDate;
        this.lastUpdate = lastUpdate;
        this.lastSuccessfulUpdate = lastSuccessfulUpdate;
    }
}

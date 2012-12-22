package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.util.sql.*;
import org.joda.time.*;

import java.sql.*;
import java.util.*;

import static io.trygvis.esper.testing.util.sql.ResultSetF.*;
import static java.lang.System.*;

public class BuildDao {
    private final Connection c;

    public static final String BUILD = "uuid, created_date, timestamp, success, reference_type, reference_uuid";

    public static final SqlF<ResultSet, BuildDto> build = new SqlF<ResultSet, BuildDto>() {
        public BuildDto apply(ResultSet rs) throws SQLException {
            int i = 1;
            return new BuildDto(
                    UUID.fromString(rs.getString(i++)),
                    new DateTime(rs.getTimestamp(i++).getTime()),
                    new DateTime(rs.getTimestamp(i++).getTime()),
                    rs.getBoolean(i++),
                    EntityRef.fromRs(rs, i));
        }
    };

    public BuildDao(Connection c) {
        this.c = c;
    }

    public UUID insertBuild(DateTime timestamp, boolean success, EntityRef ref) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO build(" + BUILD + ") VALUES(?, ?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setTimestamp(i++, new Timestamp(timestamp.getMillis()));
            s.setBoolean(i++, success);
            s.setString(i++, ref.type);
            s.setString(i, ref.uuid.toString());
            s.executeUpdate();
            return uuid;
        }
    }

    public void insertBuildParticipant(UUID build, UUID person) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO build_participant(build, person) VALUES(?, ?)")) {
            int i = 1;
            s.setString(i++, build.toString());
            s.setString(i, person.toString());
            s.executeUpdate();
        }
    }

    public List<UUID> selectPersonsFromBuildParticipant(UUID build) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT person FROM build_participant WHERE build=?")) {
            int i = 1;
            s.setString(i, build.toString());
            return Util.toList(s, getUuid);
        }
    }
}

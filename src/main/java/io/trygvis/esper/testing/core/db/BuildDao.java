package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.util.sql.*;
import org.joda.time.*;

import java.sql.*;
import java.util.*;

import static io.trygvis.esper.testing.Util.*;
import static io.trygvis.esper.testing.util.sql.ResultSetF.*;
import static io.trygvis.esper.testing.util.sql.SqlOption.fromRs;
import static java.lang.System.*;

public class BuildDao {
    private final Connection c;

    public static final String BUILD = "UUID, created_date, TIMESTAMP, success, reference_type, reference_uuid";

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

    public void insertBuildParticipant(UUID build, Uuid person) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO build_participant(build, person) VALUES(?, ?)")) {
            int i = 1;
            s.setString(i++, build.toString());
            s.setString(i, person.toUuidString());
            s.executeUpdate();
        }
    }

    public List<Uuid> selectBuildParticipantByBuild(UUID build) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT person FROM build_participant WHERE build=?")) {
            int i = 1;
            s.setString(i, build.toString());
            return toList(s, getUuid);
        }
    }

    public List<PersonDto> selectPersonsFromBuildParticipant(UUID build) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PersonDao.PERSON + " FROM person p, build_participant bp WHERE bp.person = p.uuid AND build=?")) {
            int i = 1;
            s.setString(i, build.toString());
            return toList(s, PersonDao.person);
        }
    }

    public SqlOption<BuildDto> selectBuild(UUID uuid) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + BUILD + " FROM build WHERE uuid=?")) {
            int i = 1;
            s.setString(i, uuid.toString());
            return fromRs(s.executeQuery()).map(build);
        }
    }

    public SqlOption<UUID> findBuildByReference(EntityRef ref) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT uuid FROM build WHERE reference_type=? AND reference_uuid=?")) {
            int i = 1;
            s.setString(i++, ref.type);
            s.setString(i, ref.uuid.toString());
            return fromRs(s.executeQuery()).map(getUUID);
        }
    }

    public List<BuildDto> selectBuildsByPerson(Uuid person, PageRequest page) throws SQLException {
        String sql = "SELECT " + BUILD + " FROM build b, build_participant bp WHERE bp.person=? AND b.uuid = bp.build";
        sql += orderBy(page.orderBy, "created_date", "timestamp");
        sql += " LIMIT ? OFFSET ?";

        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i = 1;
            s.setString(i++, person.toUuidString());
            s.setInt(i++, page.count.orSome(10));
            s.setInt(i, page.startIndex.orSome(0));
            return toList(s, build);
        }
    }

    public List<BuildDto> selectBuilds(PageRequest page) throws SQLException {
        String sql = "SELECT " + BUILD + " FROM build";
        sql += orderBy(page.orderBy, "created_date", "timestamp");
        sql += " LIMIT ? OFFSET ?";

        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i = 1;
            s.setInt(i++, page.count.orSome(10));
            s.setInt(i, page.startIndex.orSome(0));
            return toList(s, build);
        }
    }
}

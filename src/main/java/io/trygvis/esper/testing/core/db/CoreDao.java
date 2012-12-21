package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.util.sql.*;
import org.joda.time.*;

import java.sql.*;
import java.util.*;

import static io.trygvis.esper.testing.util.sql.SqlOption.*;
import static java.lang.System.*;

public class CoreDao {
    private final Connection c;

    public static final String PERSON = "uuid, created_date, name";

    public static final SqlF<ResultSet, PersonDto> person = new SqlF<ResultSet, PersonDto>() {
        public PersonDto apply(ResultSet rs) throws SQLException {
            int i = 1;
            return new PersonDto(
                    UUID.fromString(rs.getString(i++)),
                    new DateTime(rs.getTimestamp(i++).getTime()),
                    rs.getString(i));
        }
    };

    public static final String BUILD = "uuid, created_date, timestamp, success, reference_type, reference_uuid";

    public CoreDao(Connection c) {
        this.c = c;
    }

    public SqlOption<PersonDto> selectPerson(String id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON + " FROM person WHERE id=?")) {
            int i = 1;
            s.setString(i, id);
            return fromRs(s.executeQuery()).map(person);
        }
    }

    public SqlOption<PersonDto> selectPersonByJenkinsUuid(UUID jenkinsUser) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON + " FROM person WHERE uuid=(SELECT person FROM person_jenkins_user WHERE jenkins_user=?)")) {
            int i = 1;
            s.setString(i, jenkinsUser.toString());
            return fromRs(s.executeQuery()).map(person);
        }
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
}

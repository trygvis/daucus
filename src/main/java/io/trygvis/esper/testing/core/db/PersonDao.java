package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.util.sql.*;
import org.joda.time.*;

import java.sql.*;
import java.util.*;

import static io.trygvis.esper.testing.util.sql.SqlOption.*;
import static java.lang.System.*;

public class PersonDao {
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

    public static final String PERSON_BADGE = "uuid, created_date, person, name, level, count";

    public static final SqlF<ResultSet, PersonBadgeDto> personBadge = new SqlF<ResultSet, PersonBadgeDto>() {
        public PersonBadgeDto apply(ResultSet rs) throws SQLException {
            int i = 1;
            return new PersonBadgeDto(
                    UUID.fromString(rs.getString(i++)),
                    new DateTime(rs.getTimestamp(i++).getTime()),
                    UUID.fromString(rs.getString(i++)),
                    PersonBadgeDto.Type.valueOf(rs.getString(i++)),
                    rs.getInt(i),
                    rs.getInt(i));
        }
    };

    public static final String PERSON_BADGE_PROGRESS = "uuid, created_date, person, badge, state";

    public static final SqlF<ResultSet, PersonBadgeProgressDto> personBadgeProgress = new SqlF<ResultSet, PersonBadgeProgressDto>() {
        public PersonBadgeProgressDto apply(ResultSet rs) throws SQLException {
            int i = 1;
            return new PersonBadgeProgressDto(
                    UUID.fromString(rs.getString(i++)),
                    new DateTime(rs.getTimestamp(i++).getTime()),
                    UUID.fromString(rs.getString(i++)),
                    rs.getString(i++),
                    rs.getString(i));
        }
    };

    public PersonDao(Connection c) {
        this.c = c;
    }

    // -----------------------------------------------------------------------
    // Person
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Badge
    // -----------------------------------------------------------------------

    public UUID insertBadge(UUID person, PersonBadgeDto.Type type, int level, int count) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO person_badge(" + PERSON_BADGE + ") VALUES(?, ?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, person.toString());
            s.setString(i++, type.toString());
            s.setInt(i++, level);
            s.setInt(i, count);
            s.executeUpdate();
            return uuid;
        }
    }

    public void incrementBadgeCount(UUID person, PersonBadgeDto.Type type, int level) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE person_badge SET count=count+1 WHERE person=? AND name=? AND level=?")) {
            int i = 1;
            s.setString(i++, person.toString());
            s.setString(i++, type.toString());
            s.setInt(i, level);
            s.executeUpdate();
        }
    }

    public SqlOption<PersonBadgeDto> selectBadge(UUID person, PersonBadgeDto.Type type, int level) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON_BADGE + " FROM person_badge WHERE person=? AND name=? AND level=?")) {
            int i = 1;
            s.setString(i++, person.toString());
            s.setString(i++, type.toString());
            s.setInt(i, level);
            return fromRs(s.executeQuery()).map(personBadge);
        }
    }

    // -----------------------------------------------------------------------
    // Badge Progress
    // -----------------------------------------------------------------------

    public SqlOption<PersonBadgeProgressDto> selectBadgeProgress(UUID person, PersonBadgeDto.Type type) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON_BADGE_PROGRESS + " FROM person_badge_progress WHERE person=? AND badge=?")) {
            int i = 1;
            s.setString(i++, person.toString());
            s.setString(i, type.toString());
            return fromRs(s.executeQuery()).map(personBadgeProgress);
        }
    }

    public void insertBadgeProgress(UUID person, PersonBadgeDto.Type type, String state) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO person_badge_progress (" + PERSON_BADGE_PROGRESS + ") VALUES(?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, person.toString());
            s.setString(i++, type.toString());
            s.setString(i, state);
            s.executeUpdate();
        }
    }

    public void updateBadgeProgress(UUID person, PersonBadgeDto.Type type, String state) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE person_badge_progress SET state=? WHERE person=? AND badge=?")) {
            int i = 1;
            s.setString(i++, state);
            s.setString(i++, person.toString());
            s.setString(i, type.toString());
            s.executeUpdate();
        }
    }
}

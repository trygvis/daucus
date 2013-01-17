package io.trygvis.esper.testing.core.db;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.db.PersonalBadgeDto.*;
import io.trygvis.esper.testing.util.sql.*;
import org.joda.time.*;

import java.sql.*;
import java.util.List;
import java.util.*;

import static io.trygvis.esper.testing.Util.*;
import static io.trygvis.esper.testing.util.sql.ResultSetF.getUUID;
import static io.trygvis.esper.testing.util.sql.SqlOption.*;
import static java.lang.System.*;

public class PersonDao {
    private final Connection c;

    public static final String PERSON = "uuid, created_date, name, mail";

    public static final SqlF<ResultSet, PersonDto> person = new SqlF<ResultSet, PersonDto>() {
        public PersonDto apply(ResultSet rs) throws SQLException {
            int i = 1;
            return new PersonDto(
                    Uuid.fromString(rs.getString(i++)),
                    new DateTime(rs.getTimestamp(i++).getTime()),
                    rs.getString(i++),
                    rs.getString(i));
        }
    };

    public static final String PERSON_BADGE = "uuid, created_date, person, name, level, state";

    public static final SqlF<ResultSet, PersonalBadgeDto> personBadge = new SqlF<ResultSet, PersonalBadgeDto>() {
        public PersonalBadgeDto apply(ResultSet rs) throws SQLException {
            int i = 1;
            return new PersonalBadgeDto(
                    UUID.fromString(rs.getString(i++)),
                    new DateTime(rs.getTimestamp(i++).getTime()),
                    Uuid.fromString(rs.getString(i++)),
                    BadgeType.valueOf(rs.getString(i++)),
                    rs.getInt(i++),
                    rs.getString(i));
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

    public Uuid insertPerson(String mail, String name) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO person(" + PERSON + ") VALUES(?, ?, ?, ?)")) {
            Uuid uuid = Uuid.randomUuid();
            int i = 1;
            s.setString(i++, uuid.toUuidString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, name);
            s.setString(i, mail);
            s.executeUpdate();
            return uuid;
        }
    }

    public SqlOption<PersonDto> selectPerson(Uuid uuid) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON + " FROM person WHERE uuid=?")) {
            int i = 1;
            s.setString(i, uuid.toUuidString());
            return fromRs(s.executeQuery()).map(person);
        }
    }

    public SqlOption<PersonDto> selectPersonByMail(String mail) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON + " FROM person WHERE mail=?")) {
            int i = 1;
            s.setString(i, mail);
            return fromRs(s.executeQuery()).map(person);
        }
    }

    public List<PersonDto> selectPersons(PageRequest pageRequest, Option<String> query) throws SQLException {
        String sql = "SELECT " + PERSON + " FROM person";

        if (query.isSome()) {
            sql += " WHERE lower(name) LIKE '%' || ? || '%'";
        }

        sql += orderBy(pageRequest.orderBy, "name", "created_date");

        sql += " LIMIT ? OFFSET ?";

        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i = 1;

            if (query.isSome()) {
                s.setString(i++, query.some());
            }

            s.setInt(i++, pageRequest.count.orSome(10));
            s.setInt(i, pageRequest.startIndex.orSome(0));
            return toList(s, person);
        }
    }

    public int selectPersonCount() throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT count(1) FROM person")) {
            return fromRs(s.executeQuery()).map(ResultSetF.getInt).get();
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
    // Person Jenkins User
    // -----------------------------------------------------------------------

    public void insertPersonJenkinsUser(Uuid person, UUID jenkinsUser) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO person_jenkins_user(person, jenkins_user) VALUES(?, ?)")) {
            int i = 1;
            s.setString(i++, person.toUuidString());
            s.setString(i, jenkinsUser.toString());
            s.executeUpdate();
        }
    }

    public List<UUID> selectJenkinsUserUuidsByPerson(Uuid person) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT jenkins_user FROM person_jenkins_user WHERE person=?")) {
            int i = 1;
            s.setString(i, person.toUuidString());
            return toList(s, getUUID);
        }
    }

    public boolean hasPersonJenkinsUser(Uuid person, UUID jenkinsUser) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM person_jenkins_user WHERE person=? AND jenkins_user=?")) {
            int i = 1;
            s.setString(i++, person.toUuidString());
            s.setString(i, jenkinsUser.toString());
            ResultSet rs = s.executeQuery();
            return rs.next();
        }
    }

    // -----------------------------------------------------------------------
    // Badge
    // -----------------------------------------------------------------------

    public UUID insertBadge(DateTime createdDate, Uuid person, BadgeType type, int level, String state) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO person_badge(" + PERSON_BADGE + ") VALUES(?, ?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(createdDate.getMillis()));
            s.setString(i++, person.toUuidString());
            s.setString(i++, type.toString());
            s.setInt(i++, level);
            s.setString(i, state);
            s.executeUpdate();
            return uuid;
        }
    }

    public List<PersonalBadgeDto> selectBadges(Uuid person) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON_BADGE + " FROM person_badge WHERE person=? ORDER BY created_date DESC")) {
            int i = 1;
            s.setString(i, person.toUuidString());
            return toList(s, personBadge);
        }
    }

    public SqlOption<PersonalBadgeDto> selectBadge(Uuid uuid) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON_BADGE + " FROM person_badge WHERE uuid=?")) {
            int i = 1;
            s.setString(i, uuid.toUuidString());
            return fromRs(s.executeQuery()).map(personBadge);
        }
    }

    public List<PersonalBadgeDto> selectBadges(Option<Uuid> person, Option<BadgeType> type, Option<Integer> level, PageRequest page) throws SQLException {
        String sql = "SELECT " + PERSON_BADGE + " FROM person_badge WHERE 1=1";

        if (person.isSome()) {
            sql += " AND person=?";
        }

        if (type.isSome()) {
            sql += " AND name=?";
        }

        if (level.isSome()) {
            sql += " AND level=?";
        }

        sql += orderBy(page.orderBy, "name", "created_date");

        sql += " LIMIT ? OFFSET ?";

        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i = 1;
            if (person.isSome()) {
                s.setString(i++, person.some().toUuidString());
            }
            if (type.isSome()) {
                s.setString(i++, type.some().toString());
            }
            if (level.isSome()) {
                s.setInt(i, level.some());
            }
            s.setInt(i++, page.count.orSome(10));
            s.setInt(i, page.startIndex.orSome(0));
            return toList(s, personBadge);
        }
    }

    // -----------------------------------------------------------------------
    // Badge Progress
    // -----------------------------------------------------------------------

    public SqlOption<PersonBadgeProgressDto> selectBadgeProgress(Uuid person, BadgeType type) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON_BADGE_PROGRESS + " FROM person_badge_progress WHERE person=? AND badge=?")) {
            int i = 1;
            s.setString(i++, person.toUuidString());
            s.setString(i, type.toString());
            return fromRs(s.executeQuery()).map(personBadgeProgress);
        }
    }

    public List<PersonBadgeProgressDto> selectBadgeProgresses(Uuid person) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + PERSON_BADGE_PROGRESS + " FROM person_badge_progress WHERE person=? ORDER BY badge")) {
            int i = 1;
            s.setString(i, person.toUuidString());
            return toList(s, personBadgeProgress);
        }
    }

    public void insertBadgeProgress(Uuid person, BadgeType type, String state) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO person_badge_progress (" + PERSON_BADGE_PROGRESS + ") VALUES(?, ?, ?, ?, ?)")) {
            UUID uuid = UUID.randomUUID();
            int i = 1;
            s.setString(i++, uuid.toString());
            s.setTimestamp(i++, new Timestamp(currentTimeMillis()));
            s.setString(i++, person.toUuidString());
            s.setString(i++, type.toString());
            s.setString(i, state);
            s.executeUpdate();
        }
    }

    public void updateBadgeProgress(Uuid person, BadgeType type, String state) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE person_badge_progress SET state=? WHERE person=? AND badge=?")) {
            int i = 1;
            s.setString(i++, state);
            s.setString(i++, person.toUuidString());
            s.setString(i, type.toString());
            s.executeUpdate();
        }
    }
}

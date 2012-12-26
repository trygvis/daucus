package io.trygvis.esper.testing.web;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.badge.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.util.sql.*;

import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import static io.trygvis.esper.testing.util.sql.PageRequest.*;
import static io.trygvis.esper.testing.web.JenkinsResource.*;

@Path("/resource/core")
@Produces(MediaType.APPLICATION_JSON)
public class CoreResource extends AbstractResource {

    private final BadgeService badgeService;

    public CoreResource(DatabaseAccess da, BadgeService badgeService) {
        super(da);
        this.badgeService = badgeService;
    }

    @GET
    @Path("/person")
    public List<PersonJson> getPersons(@Context final HttpServletRequest req) throws Exception {
        final PageRequest pageRequest = pageReq(req);

        return da.inTransaction(new DatabaseAccess.DaosCallback<List<PersonJson>>() {
            public List<PersonJson> run(Daos daos) throws SQLException {
                List<PersonJson> list = new ArrayList<>();
                for (PersonDto person : daos.personDao.selectPerson(pageRequest)) {
                    list.add(getPersonJson(daos, person));
                }
                return list;
            }
        });
    }

    /**
     * This is wrong, but Angular's $resource is a bit dumb.
     */
    @GET
    @Path("/person-count")
    public int getPersonCount() throws Exception {
        return da.inTransaction(new DatabaseAccess.DaosCallback<Integer>() {
            public Integer run(Daos daos) throws SQLException {
                return daos.personDao.selectPersonCount();
            }
        });
    }

    @GET
    @Path("/person/{uuid}")
    public PersonJson getPerson(@PathParam("uuid") final String s) throws Exception {
        final UUID uuid = parseUuid(s);

        return get(new DatabaseAccess.DaosCallback<Option<PersonJson>>() {
            public Option<PersonJson> run(Daos daos) throws SQLException {
                SqlOption<PersonDto> o = daos.personDao.selectPerson(uuid);
                if (o.isNone()) {
                    return Option.none();
                }

                return Option.some(getPersonJson(daos, o.get()));
            }
        });
    }

    private PersonJson getPersonJson(Daos daos, PersonDto person) throws SQLException {
        List<BadgeJson> badges = new ArrayList<>();

        for (PersonBadgeDto badge : daos.personDao.selectBadges(person.uuid)) {
            badges.add(new BadgeJson(badge.type.name(), badge.level, badge.count, 100, 100));
        }

        List<BadgeJson> badgesInProgress = new ArrayList<>();

        for (PersonBadgeProgressDto badgeProgressDto : daos.personDao.selectBadgeProgresses(person.uuid)) {
            UnbreakableBadgeProgress progress = badgeService.unbreakable(badgeProgressDto);
            badgesInProgress.add(new BadgeJson(progress.type.name(), progress.progressingAgainstLevel(), 0,
                    progress.progression(), progress.goal()));
        }

        return new PersonJson(
                person.uuid,
                person.name,
                badges,
                badgesInProgress
        );
    }

    public static class PersonJson {
        public final UUID uuid;
        public final String name;
        public final List<BadgeJson> badges;
        public final List<BadgeJson> badgesInProgress;

        public PersonJson(UUID uuid, String name, List<BadgeJson> badges, List<BadgeJson> badgesInProgress) {
            this.uuid = uuid;
            this.name = name;
            this.badges = badges;
            this.badgesInProgress = badgesInProgress;
        }
    }

    public static class BadgeJson {
        public final String name;
        public final int level;

        /**
         * Number of times this badge has been received.
         */
        public final int count;
        public final int progress;
        public final int goal;

        public BadgeJson(String name, int level, int count, int progress, int goal) {
            this.name = name;
            this.level = level;
            this.count = count;
            this.progress = progress;
            this.goal = goal;
        }
    }
}

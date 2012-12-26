package io.trygvis.esper.testing.web;

import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.badge.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.util.sql.*;

import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import java.util.*;

@Path("/resource/core")
public class CoreResource {

    private final DatabaseAccess da;
    private final BadgeService badgeService;

    public CoreResource(DatabaseAccess da, BadgeService badgeService) {
        this.da = da;
        this.badgeService = badgeService;
    }

    @GET
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PersonJson> getServers(@Context final HttpServletRequest req) throws Exception {
        return da.inTransaction(new DatabaseAccess.DaosCallback<List<PersonJson>>() {
            @Override
            public List<PersonJson> run(Daos daos) throws SQLException {
                List<PersonJson> list = new ArrayList<>();
                for (PersonDto person : daos.personDao.selectPerson(PageRequest.fromReq(req))) {
                    list.add(getPersonJson(daos, person));
                }
                return list;
            }
        });
    }

    private PersonJson getPersonJson(Daos daos, PersonDto person) throws SQLException {
        List<BadgeJson> badges = new ArrayList<>();

        for (PersonBadgeDto badge : daos.personDao.selectBadges(person.uuid)) {
            badges.add(new BadgeJson(badge.type.name(), badge.level, badge.count, 100, 100));
        }

        for (PersonBadgeProgressDto badgeProgressDto : daos.personDao.selectBadgeProgresses(person.uuid)) {
            UnbreakableBadgeProgress progress = badgeService.unbreakable(badgeProgressDto);
            badges.add(new BadgeJson(progress.type.name(), progress.progressingAgainstLevel(), 0,
                progress.progression(), progress.goal()));
        }

        return new PersonJson(
            person.uuid,
            person.name,
            badges
        );
    }

    public static class PersonJson {
        public final UUID uuid;
        public final String name;
        public final List<BadgeJson> badges;

        public PersonJson(UUID uuid, String name, List<BadgeJson> badges) {
            this.uuid = uuid;
            this.name = name;
            this.badges = badges;
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

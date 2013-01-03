package io.trygvis.esper.testing.web.resource;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.badge.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.util.sql.*;
import io.trygvis.esper.testing.web.*;
import org.joda.time.*;

import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import static io.trygvis.esper.testing.util.sql.PageRequest.*;

@Path("/resource/core")
@Produces(MediaType.APPLICATION_JSON)
public class CoreResource extends AbstractResource {

    private final BadgeService badgeService;

    public CoreResource(DatabaseAccess da, BadgeService badgeService) {
        super(da);
        this.badgeService = badgeService;
    }

    // -----------------------------------------------------------------------
    // Person
    // -----------------------------------------------------------------------

    @GET
    @Path("/person")
    public List<PersonJson> getPersons(@Context final HttpServletRequest req) throws Exception {
        final PageRequest pageRequest = pageReq(req);

        return da.inTransaction(new DatabaseAccess.DaosCallback<List<PersonJson>>() {
            public List<PersonJson> run(Daos daos) throws SQLException {
                List<PersonJson> list = new ArrayList<>();
                for (PersonDto person : daos.personDao.selectPersons(pageRequest)) {
                    list.add(getPersonJson(daos, person));
                }
                return list;
            }
        });
    }

    @GET
    @Path("/person/{uuid}")
    public PersonJson getPerson(@PathParam("uuid") final String s) throws Exception {
        final UUID uuid = JenkinsResource.parseUuid(s);

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

    // -----------------------------------------------------------------------
    // Build
    // -----------------------------------------------------------------------

    @GET
    @Path("/build")
    public List<BuildJson> getBuilds(@MagicParam final PageRequest page, @MagicParam(query = "person") final UUID person) throws Exception {
        return da.inTransaction(new DatabaseAccess.DaosCallback<List<BuildJson>>() {
            public List<BuildJson> run(Daos daos) throws SQLException {
                List<BuildDto> buildDtos;

                if (person != null) {
                    buildDtos = daos.buildDao.selectBuildsByPerson(person, page);
                } else {
                    buildDtos = daos.buildDao.selectBuilds(page);
                }

                List<BuildJson> list = new ArrayList<>();
                for (BuildDto build : buildDtos) {
                    list.add(getBuildJson(build));
                }
                return list;
            }
        });
    }

    @GET
    @Path("/build-participant/{uuid}")
    public List<PersonJson> getBuildParticipants(@MagicParam final UUID build) throws Exception {
        return da.inTransaction(new DatabaseAccess.DaosCallback<List<PersonJson>>() {
            public List<PersonJson> run(Daos daos) throws SQLException {
                List<PersonJson> list = new ArrayList<>();
                for (PersonDto person : daos.buildDao.selectPersonsFromBuildParticipant(build)) {
                    list.add(getPersonJson(daos, person));
                }
                return list;
            }
        });
    }

    @GET
    @Path("/build/{uuid}")
    public BuildJson getBuild(@MagicParam final UUID uuid) throws Exception {
        return get(new DatabaseAccess.DaosCallback<Option<BuildJson>>() {
            public Option<BuildJson> run(Daos daos) throws SQLException {
                SqlOption<BuildDto> o = daos.buildDao.selectBuild(uuid);
                if (o.isNone()) {
                    return Option.none();
                }

                return Option.some(getBuildJson(o.get()));
            }
        });
    }

    private BuildJson getBuildJson(BuildDto build) {
        return new BuildJson(build.uuid, build.timestamp, build.success);
    }

    public static class BuildJson {
        public final UUID uuid;
        public final DateTime timestamp;
        public final boolean success;

        public BuildJson(UUID uuid, DateTime timestamp, boolean success) {
            this.uuid = uuid;
            this.timestamp = timestamp;
            this.success = success;
        }
    }
}

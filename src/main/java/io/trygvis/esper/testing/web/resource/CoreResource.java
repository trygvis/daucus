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

import static fj.data.Option.fromNull;
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
    public List<PersonDetailJson> getPersons(@Context final HttpServletRequest req) throws Exception {
        final PageRequest pageRequest = pageReq(req);

        return da.inTransaction(new CoreDaosCallback<List<PersonDetailJson>>() {
            protected List<PersonDetailJson> run() throws SQLException {
                List<PersonDetailJson> list = new ArrayList<>();
                for (PersonDto person : daos.personDao.selectPersons(pageRequest)) {
                    list.add(super.getPersonDetailJson.apply(person));
                }
                return list;
            }
        });
    }

    @GET
    @Path("/person/{uuid}")
    public PersonDetailJson getPerson(@PathParam("uuid") final Uuid uuid) throws Exception {
        return sql(new CoreDaosCallback<SqlOption<PersonDetailJson>>() {
            protected SqlOption<PersonDetailJson> run() throws SQLException {
                return daos.personDao.selectPerson(uuid).map(super.getPersonDetailJson);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Build
    // -----------------------------------------------------------------------

    @GET
    @Path("/build")
    public List<BuildJson> getBuilds(@MagicParam final PageRequest page, @MagicParam(query = "person") final Uuid person) throws Exception {
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
    public List<PersonDetailJson> getBuildParticipants(@MagicParam final UUID build) throws Exception {
        return da.inTransaction(new CoreDaosCallback<List<PersonDetailJson>>() {
            protected List<PersonDetailJson> run() throws SQLException {
                List<PersonDetailJson> list = new ArrayList<>();
                for (PersonDto person : daos.buildDao.selectPersonsFromBuildParticipant(build)) {
                    list.add(super.getPersonDetailJson.apply(person));
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

    // -----------------------------------------------------------------------
    // Badge
    // -----------------------------------------------------------------------

    @GET
    @Path("/badge")
    public List<BadgeJson> getBadges(@MagicParam final PageRequest page, @MagicParam(query = "person") final Uuid person) throws Exception {
        return da.inTransaction(new CoreDaosCallback<List<BadgeJson>>() {
            protected List<BadgeJson> run() throws SQLException {
                List<PersonalBadgeDto> badgeDtos = daos.personDao.selectBadges(fromNull(person), Option.<PersonalBadgeDto.BadgeType>none(), Option.<Integer>none(), page);

                List<BadgeJson> list = new ArrayList<>();
                for (PersonalBadgeDto badge : badgeDtos) {
                    list.add(getBadgeJson.apply(badge));
                }
                return list;
            }
        });
    }

    abstract class CoreDaosCallback<T> implements DatabaseAccess.DaosCallback<T> {
        protected Daos daos;

        protected abstract T run() throws SQLException;

        public T run(Daos daos) throws SQLException {
            this.daos = daos;
            return run();
        }

        protected final SqlF<PersonDto, PersonJson> getPersonJson = new SqlF<PersonDto, PersonJson>() {
            public PersonJson apply(PersonDto person) throws SQLException {
                return new PersonJson(person.uuid, person.name);
            }
        };

        protected final SqlF<PersonDto, PersonDetailJson> getPersonDetailJson = new SqlF<PersonDto, PersonDetailJson>() {
            public PersonDetailJson apply(PersonDto person) throws SQLException {
                List<BadgeJson> badges = new ArrayList<>();
                for (PersonalBadgeDto badge : daos.personDao.selectBadges(person.uuid)) {
                    badges.add(getBadgeJson.apply(badge));
                }

                List<BadgeJson> badgesInProgress = new ArrayList<>();
                for (PersonBadgeProgressDto badgeProgressDto : daos.personDao.selectBadgeProgresses(person.uuid)) {
                    BadgeProgress progress = badgeService.badgeProgress(badgeProgressDto);
                    badgesInProgress.add(getBadge(progress));
                }

                return new PersonDetailJson(
                        getPersonJson.apply(person),
                        badges,
                        badgesInProgress
                );
            }
        };

        protected SqlF<PersonalBadgeDto, BadgeJson> getBadgeJson = new SqlF<PersonalBadgeDto, BadgeJson>() {
            public BadgeJson apply(PersonalBadgeDto badge) throws SQLException {
                return new BadgeJson(badge.createdDate, badge.type.name(), badge.level);
            }
        };

        private BadgeJson getBadge(BadgeProgress progress) {
            return new BadgeJson(null, progress.type.name(), progress.progressingAgainstLevel(), progress.progression(), progress.goal());
        }

        protected final SqlF<PersonalBadgeDto, BadgeDetailJson> getBadgeDetailJson = new SqlF<PersonalBadgeDto, BadgeDetailJson>() {
            public BadgeDetailJson apply(PersonalBadgeDto badgeDto) throws SQLException {
                return new BadgeDetailJson(getBadgeJson.apply(badgeDto),
                        daos.personDao.selectPerson(badgeDto.person).map(getPersonJson).get());
            }
        };
    }
}

class BuildJson {
    public final UUID uuid;
    public final DateTime timestamp;
    public final boolean success;

    public BuildJson(UUID uuid, DateTime timestamp, boolean success) {
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.success = success;
    }
}

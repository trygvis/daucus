package io.trygvis.esper.testing.web.resource;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.jenkins.*;
import io.trygvis.esper.testing.util.sql.*;
import io.trygvis.esper.testing.web.*;
import org.joda.time.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import static fj.data.Option.*;

@Path("/resource/jenkins")
public class JenkinsResource extends AbstractResource {

    public JenkinsResource(DatabaseAccess da) {
        super(da);
    }

    @GET
    @Path("/server")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JenkinsServerJson> getServers() throws Exception {
        return da.inTransaction(new DatabaseAccess.DaosCallback<List<JenkinsServerJson>>() {
            public List<JenkinsServerJson> run(Daos daos) throws SQLException {
                List<JenkinsServerJson> list = new ArrayList<>();
                for (JenkinsServerDto server : daos.jenkinsDao.selectServers(false)) {
                    list.add(getJenkinsServerJson(daos, server));
                }
                return list;
            }
        });
    }

    @GET
    @Path("/server/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JenkinsServerJson getServer(@PathParam("uuid") String s) throws Exception {
        final UUID uuid = parseUuid(s);

        return get(new DatabaseAccess.DaosCallback<Option<JenkinsServerJson>>() {
            public Option<JenkinsServerJson> run(final Daos daos) throws SQLException {
                Option<JenkinsServerDto> o = daos.jenkinsDao.selectServer(uuid);

                if (o.isNone()) {
                    return Option.none();
                }

                return some(getJenkinsServerJson(daos, o.some()));
            }
        });
    }

    @GET
    @Path("/job")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JenkinsJobJson> getJobs(@MagicParam(query = "server") final UUID server, @MagicParam final PageRequest page) throws Exception {
        return da.inTransaction(new DatabaseAccess.DaosCallback<List<JenkinsJobJson>>() {
            public List<JenkinsJobJson> run(final Daos daos) throws SQLException {
                List<JenkinsJobJson> jobs = new ArrayList<>();
                for (JenkinsJobDto job : daos.jenkinsDao.selectJobsByServer(server, page)) {
                    jobs.add(getJenkinsJobJson(job));
                }
                return jobs;
            }
        });
    }

    private JenkinsServerJson getJenkinsServerJson(Daos daos, JenkinsServerDto server) throws SQLException {
        int count = daos.jenkinsDao.selectJobCountForServer(server.uuid);

        List<JenkinsJobJson> jobs = new ArrayList<>();
        for (JenkinsJobDto jobDto : daos.jenkinsDao.selectJobsByServer(server.uuid, PageRequest.FIRST_PAGE)) {
            jobs.add(getJenkinsJobJson(jobDto));
        }

        return new JenkinsServerJson(server.uuid, server.createdDate, server.url, server.enabled, count, jobs);
    }

    private JenkinsJobJson getJenkinsJobJson(JenkinsJobDto job) {
        return new JenkinsJobJson(job.uuid, job.createdDate, job.displayName.toNull());
    }

    public static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
}

class JenkinsServerJson {
    public final UUID uuid;
    public final DateTime createdDate;
    public final URI url;
    public final boolean enabled;
    public final int jobCount;
    public final List<JenkinsJobJson> recentJobs;

    JenkinsServerJson(UUID uuid, DateTime createdDate, URI url, boolean enabled, int jobCount, List<JenkinsJobJson> recentJobs) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.url = url;
        this.enabled = enabled;
        this.jobCount = jobCount;
        this.recentJobs = recentJobs;
    }
}

class JenkinsJobJson {
    public final UUID uuid;
    public final DateTime createdDate;
    public final String displayName;

    JenkinsJobJson(UUID uuid, DateTime createdDate, String displayName) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.displayName = displayName;
    }
}

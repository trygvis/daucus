package io.trygvis.esper.testing.web.resource;

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

@Path("/resource/jenkins")
public class JenkinsResource extends AbstractResource {

    public JenkinsResource(DatabaseAccess da) {
        super(da);
    }

    @GET
    @Path("/server")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JenkinsServerJson> getServers() throws Exception {
        return da.inTransaction(new JenkinsDaosCallback<List<JenkinsServerJson>>() {
            protected List<JenkinsServerJson> run() throws SQLException {
                List<JenkinsServerJson> list = new ArrayList<>();
                for (JenkinsServerDto server : daos.jenkinsDao.selectServers(false)) {
                    list.add(getJenkinsServerJson.apply(server));
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

        return sql(new JenkinsDaosCallback<SqlOption<JenkinsServerJson>>() {
            protected SqlOption<JenkinsServerJson> run() throws SQLException {
                return daos.jenkinsDao.selectServer(uuid).map(getJenkinsServerJson);
            }
        });
    }

    @GET
    @Path("/job")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JenkinsJobJson> getJobs(@MagicParam(query = "server") final UUID server, @MagicParam final PageRequest page) throws Exception {
        return da.inTransaction(new JenkinsDaosCallback<List<JenkinsJobJson>>() {
            protected List<JenkinsJobJson> run() throws SQLException {
                List<JenkinsJobJson> jobs = new ArrayList<>();
                for (JenkinsJobDto job : daos.jenkinsDao.selectJobsByServer(server, page)) {
                    jobs.add(getJenkinsJobJson.apply(job));
                }
                return jobs;
            }
        });
    }

    @GET
    @Path("/job/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JenkinsJobJson getJob(@MagicParam final UUID uuid) throws Exception {
        return sql(new JenkinsDaosCallback<SqlOption<JenkinsJobJson>>() {
            protected SqlOption<JenkinsJobJson> run() throws SQLException {
                return daos.jenkinsDao.selectJob(uuid).map(getJenkinsJobJsonDetail);
            }
        });
    }

    @GET
    @Path("/build")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JenkinsBuildJson> getBuilds(@MagicParam(query = "job") final UUID job, @MagicParam final PageRequest page) throws Exception {
        return da.inTransaction(new JenkinsDaosCallback<List<JenkinsBuildJson>>() {
            protected List<JenkinsBuildJson> run() throws SQLException {
                List<JenkinsBuildJson> builds = new ArrayList<>();
                for (JenkinsBuildDto dto : daos.jenkinsDao.selectBuildByJob(job, page)) {
                    builds.add(getJenkinsBuildJson.apply(dto));
                }
                return builds;
            }
        });
    }

    public static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    abstract class JenkinsDaosCallback<T> implements DatabaseAccess.DaosCallback<T> {
        protected Daos daos;

        protected abstract T run() throws SQLException;

        public T run(Daos daos) throws SQLException {
            this.daos = daos;
            return run();
        }

        protected SqlF<JenkinsServerDto, JenkinsServerJson> getJenkinsServerJson = new SqlF<JenkinsServerDto, JenkinsServerJson>() {
            public JenkinsServerJson apply(JenkinsServerDto server) throws SQLException {
                int count = daos.jenkinsDao.selectJobCountForServer(server.uuid);

                List<JenkinsJobJson> jobs = new ArrayList<>();
                for (JenkinsJobDto jobDto : daos.jenkinsDao.selectJobsByServer(server.uuid, PageRequest.FIRST_PAGE)) {
                    jobs.add(getJenkinsJobJson.apply(jobDto));
                }

                return new JenkinsServerJson(server.uuid, server.createdDate, server.url, server.enabled, count, jobs);
            }
        };

        protected SqlF<JenkinsJobDto,JenkinsJobJson> getJenkinsJobJson = new SqlF<JenkinsJobDto, JenkinsJobJson>() {
            public JenkinsJobJson apply(JenkinsJobDto job) throws SQLException {
                return new JenkinsJobJson(job.uuid, job.createdDate, job.server, job.displayName.toNull());
            }
        };

        protected SqlF<JenkinsJobDto,JenkinsJobJson> getJenkinsJobJsonDetail = new SqlF<JenkinsJobDto, JenkinsJobJson>() {
            public JenkinsJobJson apply(JenkinsJobDto job) throws SQLException {
                int buildCount = daos.jenkinsDao.selectBuildCountByJob(job.uuid);
                return new JenkinsJobJson(job.uuid, job.createdDate, job.server, job.displayName.toNull(), buildCount);
            }
        };

        protected SqlF<JenkinsBuildDto,JenkinsBuildJson> getJenkinsBuildJson = new SqlF<JenkinsBuildDto, JenkinsBuildJson>() {
            public JenkinsBuildJson apply(JenkinsBuildDto dto) throws SQLException {
                return new JenkinsBuildJson(dto.uuid, dto.createdDate, dto.result);
            }
        };
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
    public final UUID server;
    public final String displayName;

    public final Integer buildCount;

    JenkinsJobJson(UUID uuid, DateTime createdDate, UUID server, String displayName) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.server = server;
        this.displayName = displayName;
        this.buildCount = null;
    }

    JenkinsJobJson(UUID uuid, DateTime createdDate, UUID server, String displayName, int buildCount) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.server = server;
        this.displayName = displayName;
        this.buildCount = buildCount;
    }
}

class JenkinsBuildJson {
    public final UUID uuid;
    public final DateTime createdDate;
    public final String result;

    JenkinsBuildJson(UUID uuid, DateTime createdDate, String result) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.result = result;
    }
}

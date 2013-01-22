package io.trygvis.esper.testing.web.resource;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.jenkins.*;
import io.trygvis.esper.testing.jenkins.xml.*;
import io.trygvis.esper.testing.util.*;
import io.trygvis.esper.testing.util.sql.*;
import io.trygvis.esper.testing.web.*;
import org.joda.time.*;

import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import static fj.data.Option.fromNull;
import static org.apache.commons.lang.StringUtils.trimToNull;

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
    public List<JenkinsJobJson> getJobs(@MagicParam(query = "server") final UUID server,
                                        @MagicParam final PageRequest page,
                                        @QueryParam("query") final String query) throws Exception {
        if (server == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        return da.inTransaction(new JenkinsDaosCallback<List<JenkinsJobJson>>() {
            protected List<JenkinsJobJson> run() throws SQLException {
                List<JenkinsJobDto> dtos;

                dtos = daos.jenkinsDao.selectJobsByServer(server, page, fromNull(trimToNull(query)));

                List<JenkinsJobJson> jobs = new ArrayList<>();
                for (JenkinsJobDto job : dtos) {
                    jobs.add(getJenkinsJobJson.apply(job));
                }
                return jobs;
            }
        });
    }

    @GET
    @Path("/job/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JenkinsJobDetailJson getJob(@MagicParam final UUID uuid) throws Exception {
        return sql(new JenkinsDaosCallback<SqlOption<JenkinsJobDetailJson>>() {
            protected SqlOption<JenkinsJobDetailJson> run() throws SQLException {
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
                if(job != null) {
                    for (JenkinsBuildDto dto : daos.jenkinsDao.selectBuildByJob(job, page)) {
                        builds.add(getJenkinsBuildJson.apply(dto));
                    }
                }
                return builds;
            }
        });
    }

    @GET
    @Path("/build/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JenkinsBuildJsonDetail getBuild(@MagicParam final UUID build) throws Exception {
        return sql(new JenkinsDaosCallback<SqlOption<JenkinsBuildJsonDetail>>() {
            protected SqlOption<JenkinsBuildJsonDetail> run() throws SQLException {
                return daos.jenkinsDao.selectBuild(build).map(getJenkinsBuildJsonDetail);
            }
        });
    }

    @GET
    @Path("/user/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JenkinsUserJson getUser(@MagicParam final UUID user) throws Exception {
        return sql(new JenkinsDaosCallback<SqlOption<JenkinsUserJson>>() {
            protected SqlOption<JenkinsUserJson> run() throws SQLException {
                return daos.jenkinsDao.selectUser(user).map(getJenkinsUserJson);
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

    public static abstract class JenkinsDaosCallback<T> implements DatabaseAccess.DaosCallback<T> {
        protected Daos daos;

        protected abstract T run() throws SQLException;

        private static final XmlParser xmlParser = new XmlParser();

        public T run(Daos daos) throws SQLException {
            this.daos = daos;
            return run();
        }

        protected SqlF<JenkinsServerDto, JenkinsServerJson> getJenkinsServerJson = new SqlF<JenkinsServerDto, JenkinsServerJson>() {
            public JenkinsServerJson apply(JenkinsServerDto server) throws SQLException {
                int jobCount = daos.jenkinsDao.selectJobCountForServer(server.uuid);
                int buildCount = daos.jenkinsDao.selectBuildCountForServer(server.uuid);
                DateTime lastBuildTimestamp = daos.jenkinsDao.selectLastBuildForServer(server.uuid);

                return new JenkinsServerJson(server.uuid, server.createdDate, server.name, server.url, server.enabled,
                        jobCount, buildCount, lastBuildTimestamp);
            }
        };

        protected SqlF<JenkinsJobDto, JenkinsJobJson> getJenkinsJobJson = new SqlF<JenkinsJobDto, JenkinsJobJson>() {
            public JenkinsJobJson apply(JenkinsJobDto job) throws SQLException {
                return new JenkinsJobJson(job.uuid, job.createdDate, job.server, job.url, job.displayName.toNull());
            }
        };

        protected SqlF<JenkinsJobDto,JenkinsJobDetailJson> getJenkinsJobJsonDetail = new SqlF<JenkinsJobDto, JenkinsJobDetailJson>() {
            public JenkinsJobDetailJson apply(JenkinsJobDto dto) throws SQLException {
                return new JenkinsJobDetailJson(
                        getJenkinsJobJson.apply(dto),
                        daos.jenkinsDao.selectBuildCountByJob(dto.uuid));
            }
        };

        protected SqlF<JenkinsBuildDto, JenkinsBuildJson> getJenkinsBuildJson = new SqlF<JenkinsBuildDto, JenkinsBuildJson>() {
            public JenkinsBuildJson apply(JenkinsBuildDto dto) throws SQLException {
                Option<JenkinsBuildXml> xmlO = daos.fileDao.load(dto.file).toFj().
                        bind(xmlParser.parseDocument).
                        bind(JenkinsBuildXml.parse);

                if(xmlO.isNone()) {
                    return new JenkinsBuildJson(dto.uuid, dto.createdDate, dto.job, new DateTime(dto.createdDate),
                            "unknown", 0, 0);
                }

                JenkinsBuildXml xml = xmlO.some();

                return new JenkinsBuildJson(dto.uuid, dto.createdDate, dto.job, new DateTime(xml.timestamp),
                        xml.result.orSome("unknown"), xml.number, xml.duration);
            }
        };

        protected SqlF<JenkinsBuildDto,JenkinsBuildJsonDetail> getJenkinsBuildJsonDetail = new SqlF<JenkinsBuildDto, JenkinsBuildJsonDetail>() {
            public JenkinsBuildJsonDetail apply(JenkinsBuildDto dto) throws SQLException {
                List<JenkinsUserJson> users = new ArrayList<>();
                for (UUID user : dto.users) {
                    users.add(daos.jenkinsDao.selectUser(user).map(getJenkinsUserJson).get());
                }
                return new JenkinsBuildJsonDetail(
                        getJenkinsBuildJson.apply(dto),
                        users);
            }
        };

        protected SqlF<JenkinsUserDto,JenkinsUserJson> getJenkinsUserJson = new SqlF<JenkinsUserDto, JenkinsUserJson>() {
            public JenkinsUserJson apply(JenkinsUserDto dto) throws SQLException {
                return new JenkinsUserJson(dto.uuid, dto.createdDate, dto.absoluteUrl);
            }
        };
    }
}

class JenkinsServerJson {
    public final UUID uuid;
    public final DateTime createdDate;
    public final String name;
    public final URI url;
    public final boolean enabled;
    public final int jobCount;
    public final int buildCount;
    public final DateTime lastBuildTimestamp;

    JenkinsServerJson(UUID uuid, DateTime createdDate, String name, URI url, boolean enabled, int jobCount,
                      int buildCount, DateTime lastBuildTimestamp) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.name = name;
        this.url = url;
        this.enabled = enabled;
        this.jobCount = jobCount;
        this.buildCount = buildCount;
        this.lastBuildTimestamp = lastBuildTimestamp;
    }
}

class JenkinsJobJson {
    public final UUID uuid;
    public final DateTime createdDate;
    public final UUID server;
    public final URI url;
    public final String displayName;

    JenkinsJobJson(UUID uuid, DateTime createdDate, UUID server, URI url, String displayName) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.server = server;
        this.url = url;
        this.displayName = displayName;
    }
}

class JenkinsJobDetailJson {
    public final JenkinsJobJson job;
    public final Integer buildCount;

    JenkinsJobDetailJson(JenkinsJobJson job, Integer buildCount) {
        this.job = job;
        this.buildCount = buildCount;
    }
}

class JenkinsBuildJson {
    public final UUID uuid;
    public final DateTime createdDate;
    public final UUID job;
    public final DateTime timestamp;
    public final String result;
    public final int number;
    public final int duration;

    JenkinsBuildJson(UUID uuid, DateTime createdDate, UUID job, DateTime timestamp, String result, int number, int duration) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.job = job;
        this.timestamp = timestamp;
        this.result = result;
        this.number = number;
        this.duration = duration;
    }
}

class JenkinsBuildJsonDetail {
    public final JenkinsBuildJson build;
    public final List<JenkinsUserJson> participants;

    JenkinsBuildJsonDetail(JenkinsBuildJson build, List<JenkinsUserJson> participants) {
        this.build = build;
        this.participants = participants;
    }
}

class JenkinsUserJson {
    public final UUID uuid;
    public final DateTime createdDate;
    public final String absoluteUrl;

    JenkinsUserJson(UUID uuid, DateTime createdDate, String absoluteUrl) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.absoluteUrl = absoluteUrl;
    }
}

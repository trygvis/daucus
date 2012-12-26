package io.trygvis.esper.testing.web;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.jenkins.*;
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

    private JenkinsServerJson getJenkinsServerJson(Daos daos, JenkinsServerDto server) throws SQLException {
        int count = daos.jenkinsDao.selectJobCountForServer(server.uuid);
        return new JenkinsServerJson(server.uuid, server.createdDate, server.url, server.enabled, count);
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

    JenkinsServerJson(UUID uuid, DateTime createdDate, URI url, boolean enabled, int jobCount) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.url = url;
        this.enabled = enabled;
        this.jobCount = jobCount;
    }
}

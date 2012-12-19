package io.trygvis.esper.testing.web;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.jenkins.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import java.util.*;
import java.util.List;

@Path("/")
public class JenkinsResource {

    private final DatabaseAccess da;

    public JenkinsResource(DatabaseAccess da) {
        this.da = da;
    }

    @GET
    @Path("/resource/jenkins/server")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JenkinsServerDto> getServers() throws Exception {
        System.out.println("JenkinsResource.getServers");
        return da.inTransaction(new DatabaseAccess.DaosCallback<List<JenkinsServerDto>>() {
            @Override
            public List<JenkinsServerDto> run(Daos daos) throws SQLException {
                return daos.jenkinsDao.selectServers(false);
            }
        });
    }

    @GET
    @Path("/resource/jenkins/server/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JenkinsServerDto getServer(@PathParam("uuid") String s) throws Exception {
        try {
            final UUID uuid = UUID.fromString(s);
            System.out.println("JenkinsResource.getServers");
            Option<JenkinsServerDto> server = da.inTransaction(new DatabaseAccess.DaosCallback<Option<JenkinsServerDto>>() {
                public Option<JenkinsServerDto> run(Daos daos) throws SQLException {
                    return daos.jenkinsDao.selectServer(uuid);
                }
            });

            if(server.isNone()) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            return server.some();
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
}

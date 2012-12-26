package io.trygvis.esper.testing.web;

import fj.data.*;
import io.trygvis.esper.testing.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;

public class AbstractResource {
    protected final DatabaseAccess da;

    public AbstractResource(DatabaseAccess da) {
        this.da = da;
    }

    public <T> T get(DatabaseAccess.DaosCallback<Option<T>> callback) throws SQLException {
        Option<T> server = da.inTransaction(callback);

        if(server.isNone()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return server.some();
    }
}

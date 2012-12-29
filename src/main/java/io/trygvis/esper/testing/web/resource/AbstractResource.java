package io.trygvis.esper.testing.web.resource;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.util.sql.*;

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

    public <T> T sql(DatabaseAccess.DaosCallback<SqlOption<T>> callback) throws SQLException {
        SqlOption<T> server = da.inTransaction(callback);

        if(server.isNone()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return server.get();
    }
}

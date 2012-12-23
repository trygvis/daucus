package io.trygvis.esper.testing.web;

import io.trygvis.esper.testing.*;

import javax.ws.rs.core.*;
import java.util.*;

public class JerseyApplication extends Application {

    private final DatabaseAccess da;

    public JerseyApplication() throws Exception {
        this.da = new DatabaseAccess(WebConfig.config.createBoneCp());
    }

    @Override
    public Set<Object> getSingletons() {
        return new HashSet<Object>(Arrays.asList(
                new JenkinsResource(da)
        ));
    }
}

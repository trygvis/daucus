package io.trygvis.esper.testing.web;

import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.badge.*;

import javax.ws.rs.core.*;
import java.util.*;

public class JerseyApplication extends Application {

    private final HashSet<Object> singletons;

    public JerseyApplication() throws Exception {
        DatabaseAccess da = new DatabaseAccess(WebConfig.config.createBoneCp());

        BadgeService badgeService = new BadgeService();

        singletons = new HashSet<Object>(Arrays.asList(
            new CoreResource(da, badgeService),
            new JenkinsResource(da)
        ));
    }

    public Set<Object> getSingletons() {
        return singletons;
    }
}

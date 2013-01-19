package io.trygvis.esper.testing.web;

import com.sun.jersey.api.core.*;
import com.sun.jersey.core.spi.component.*;
import com.sun.jersey.server.impl.inject.*;
import com.sun.jersey.spi.inject.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.badge.*;
import io.trygvis.esper.testing.util.sql.*;
import io.trygvis.esper.testing.web.resource.*;
import org.codehaus.jackson.map.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fj.data.List.iterableList;
import static fj.data.List.nil;
import static fj.data.Option.*;
import static io.trygvis.esper.testing.Util.parseInt;

public class JerseyApplication extends Application {

    private final HashSet<Object> singletons;

    public JerseyApplication() throws Exception {
        Config config = WebConfig.config;

        DatabaseAccess da = new DatabaseAccess(config.createBoneCp());
        ObjectMapper objectMapper = config.createObjectMapper();

        BadgeService badgeService = new BadgeService(objectMapper);

        singletons = new HashSet<>(Arrays.asList(
                new CoreResource(da, badgeService),
                new JenkinsResource(da),
                new MyObjectMapper(objectMapper)
        ));
    }

    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.<Class<?>>asList(ResourceParamInjector.class));
    }

    public Set<Object> getSingletons() {
        return singletons;
    }

    @Provider
    public static class ResourceParamInjector implements InjectableProvider<MagicParam, Type> {

        public ComponentScope getScope() {
            return ComponentScope.PerRequest;
        }

        public Injectable getInjectable(final ComponentContext ic, final MagicParam a, Type type) {
            if (PageRequest.class.equals(type)) {
                return new AbstractHttpContextInjectable() {
                    public Object getValue(HttpContext hc) {
                        MultivaluedMap<String, String> queryParameters = hc.getRequest().getQueryParameters();

                        List<String> list = queryParameters.get("orderBy");

                        List<String> orderBy = Collections.emptyList();

                        if (list != null) {
                            orderBy = list;
                        }

                        return new PageRequest(
                                fromNull(queryParameters.getFirst("startIndex")).bind(parseInt),
                                fromNull(queryParameters.getFirst("count")).bind(parseInt),
                                orderBy);
                    }
                };
            } else if (UUID.class.equals(type)) {

                return new AbstractHttpContextInjectable() {
                    public Object getValue(HttpContext hc) {

                        if (a.query().length() > 0) {
                            return parse(hc.getRequest().getQueryParameters().getFirst(a.query()));
                        } else {
                            MultivaluedMap<String, String> pathParameters = hc.getUriInfo().getPathParameters();

                            for (Map.Entry<String, List<String>> entry : pathParameters.entrySet()) {
                                if ("uuid".equals(entry.getKey())) {
                                    return parse(entry.getValue().get(0));
                                }
                            }
                        }

                        throw new RuntimeException("@MagicParam used with UUID argument with no {uuid} path variable.");
                    }

                    private UUID parse(String s) {
                        if(s == null) {
                            return null;
                        }

                        try {
                            return UUID.fromString(s);
                        } catch (IllegalArgumentException e) {
                            throw new WebApplicationException(400);
                        }
                    }
                };
            } else if (Uuid.class.equals(type)) {

                return new AbstractHttpContextInjectable() {
                    public Object getValue(HttpContext hc) {

                        if (a.query().length() > 0) {
                            return parse(hc.getRequest().getQueryParameters().getFirst(a.query()));
                        } else {
                            MultivaluedMap<String, String> pathParameters = hc.getUriInfo().getPathParameters();

                            for (Map.Entry<String, List<String>> entry : pathParameters.entrySet()) {
                                if ("uuid".equals(entry.getKey())) {
                                    return parse(entry.getValue().get(0));
                                }
                            }
                        }

                        throw new RuntimeException("@MagicParam used with Uuid argument with no {uuid} path variable.");
                    }

                    private Uuid parse(String s) {
                        if(s == null) {
                            return null;
                        }

                        try {
                            return Uuid.fromString(s);
                        } catch (IllegalArgumentException e) {
                            throw new WebApplicationException(400);
                        }
                    }
                };
            }

            return null;
        }
    }
}

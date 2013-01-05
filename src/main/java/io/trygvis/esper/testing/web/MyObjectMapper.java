package io.trygvis.esper.testing.web;

import org.codehaus.jackson.map.*;

import javax.ws.rs.ext.*;

public class MyObjectMapper implements ContextResolver<ObjectMapper> {
    private final ObjectMapper objectMapper;

    public MyObjectMapper(ObjectMapper objectMapper) throws Exception {
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}

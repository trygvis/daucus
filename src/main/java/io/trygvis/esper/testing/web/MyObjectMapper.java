package io.trygvis.esper.testing.web;

import io.trygvis.esper.testing.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.module.*;

import java.io.*;
import javax.ws.rs.ext.*;

public class MyObjectMapper implements ContextResolver<ObjectMapper> {
    private ObjectMapper objectMapper;

    public MyObjectMapper() throws Exception {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("wat", Version.unknownVersion());
        module.addDeserializer(Uuid.class, new UuidDeserializer());
        module.addSerializer(Uuid.class, new UuidSerializer());
        objectMapper.registerModule(module);
    }

    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }

    private static class UuidDeserializer extends JsonDeserializer<Uuid> {
        public Uuid deserialize(JsonParser jp, DeserializationContext context) throws IOException {
            return Uuid.fromString(jp.getText());
        }
    }

    private static class UuidSerializer extends JsonSerializer<Uuid> {
        public void serialize(Uuid value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(value.toStringBase64());
        }
    }
}

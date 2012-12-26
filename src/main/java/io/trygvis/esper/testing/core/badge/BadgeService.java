package io.trygvis.esper.testing.core.badge;

import io.trygvis.esper.testing.core.db.*;
import org.codehaus.jackson.map.*;
import org.slf4j.*;

import java.io.*;

public class BadgeService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public UnbreakableBadgeProgress unbreakable(PersonBadgeProgressDto dto) {
        String state = dto.state;

        try {
            return objectMapper.readValue(state, UnbreakableBadgeProgress.class);
        } catch (IOException e) {
            logger.error("Could not de-serialize badge state: {}", state);
            throw new RuntimeException(e);
        }
    }

    public String serialize(UnbreakableBadgeProgress badge) {
        try {
            CharArrayWriter writer = new CharArrayWriter();
            objectMapper.writeValue(writer, badge);
            return writer.toString();
        } catch (IOException e) {
            logger.error("Could not serialize badge.", e);
            throw new RuntimeException(e);
        }
    }
}

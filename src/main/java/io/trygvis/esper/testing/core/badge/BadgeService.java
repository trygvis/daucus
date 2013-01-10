package io.trygvis.esper.testing.core.badge;

import io.trygvis.esper.testing.core.db.*;
import org.codehaus.jackson.map.*;
import org.slf4j.*;

import java.io.*;

public class BadgeService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper objectMapper;

    public BadgeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UnbreakableBadge unbreakable(PersonalBadgeDto dto) {
        return getProgress(dto.state, UnbreakableBadge.class);
    }

    // -----------------------------------------------------------------------
    // Badge
    // -----------------------------------------------------------------------

    public PersonalBadge badge(PersonalBadgeDto dto) {
        switch (dto.type) {
            case UNBREAKABLE:
                return getProgress(dto.state, UnbreakableBadge.class);
        }

        throw new RuntimeException("Unknown badge type: " + dto.type);
    }

    public <T extends PersonalBadge> T badge(PersonalBadgeDto dto, Class<T> klass) {
        switch (dto.type) {
            case UNBREAKABLE:
                if(!klass.equals(UnbreakableBadgeProgress.class)) {
                    throw new RuntimeException("Badge is not of the expected type: UNBREAKABLE.");
                }
                return getProgress(dto.state, klass);
        }

        throw new RuntimeException("Unknown badge type: " + dto.type);
    }

    // -----------------------------------------------------------------------
    // Badge Progress
    // -----------------------------------------------------------------------

    public BadgeProgress badgeProgress(PersonBadgeProgressDto dto) {
        switch (PersonalBadgeDto.BadgeType.valueOf(dto.badge)) {
            case UNBREAKABLE:
                return getProgress(dto.state, UnbreakableBadgeProgress.class);
        }

        throw new RuntimeException("Unknown badge type: " + dto.badge);
    }

    public <T extends BadgeProgress> T badgeProgress(PersonBadgeProgressDto dto, Class<T> klass) {
        switch (PersonalBadgeDto.BadgeType.valueOf(dto.badge)) {
            case UNBREAKABLE:
                if(!klass.equals(UnbreakableBadgeProgress.class)) {
                    throw new RuntimeException("Badge is not of the expected type: UNBREAKABLE.");
                }
                return getProgress(dto.state, klass);
        }

        throw new RuntimeException("Unknown badge type: " + dto.badge);
    }

    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------

    public String serialize(Object badge) {
        try {
            CharArrayWriter writer = new CharArrayWriter();
            objectMapper.writeValue(writer, badge);
            return writer.toString();
        } catch (IOException e) {
            logger.error("Could not serialize badge.", e);
            throw new RuntimeException(e);
        }
    }

    private <T> T getProgress(String state, Class<T> klass) {
        try {
            return objectMapper.readValue(state, klass);
        } catch (IOException e) {
            logger.error("Could not de-serialize badge state: {}", state);
            throw new RuntimeException(e);
        }
    }
}

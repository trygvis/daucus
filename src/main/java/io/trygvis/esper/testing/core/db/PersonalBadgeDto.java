package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.util.*;

public class PersonalBadgeDto extends AbstractEntity {
    public enum BadgeType {
        UNBREAKABLE
    }

    public final Uuid person;
    public final BadgeType type;
    public final int level;
    public final String state;

    public PersonalBadgeDto(UUID uuid, DateTime createdDate, Uuid person, BadgeType type, int level, String state) {
        super(uuid, createdDate);
        this.person = person;
        this.type = type;
        this.level = level;
        this.state = state;
    }
}

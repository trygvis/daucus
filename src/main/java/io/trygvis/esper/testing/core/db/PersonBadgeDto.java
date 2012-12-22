package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.util.*;

public class PersonBadgeDto extends AbstractEntity {
    public enum Type {
        UNBREAKABLE
    }

    public final UUID person;
    public final Type type;
    public final int level;
    public final int count;

    public PersonBadgeDto(UUID uuid, DateTime createdDate, UUID person, Type type, int level, int count) {
        super(uuid, createdDate);
        this.person = person;
        this.type = type;
        this.level = level;
        this.count = count;
    }
}

package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.util.*;

public class PersonBadgeProgressDto extends AbstractEntity {

    public final UUID person;
    public final String badge;
    public final String state;

    public PersonBadgeProgressDto(UUID uuid, DateTime createdDate, UUID person, String badge, String state) {
        super(uuid, createdDate);
        this.person = person;
        this.badge = badge;
        this.state = state;
    }
}

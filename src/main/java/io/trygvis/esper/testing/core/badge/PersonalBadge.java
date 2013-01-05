package io.trygvis.esper.testing.core.badge;

import io.trygvis.esper.testing.*;

public class PersonalBadge {
    public final Uuid person;
    public final int level;

    public PersonalBadge(Uuid person, int level) {
        this.person = person;
        this.level = level;
    }
}

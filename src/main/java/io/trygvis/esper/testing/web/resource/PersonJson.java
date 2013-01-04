package io.trygvis.esper.testing.web.resource;

import io.trygvis.esper.testing.*;

import java.util.*;

public class PersonJson {
    public final Uuid uuid;
    public final String name;

    public PersonJson(Uuid uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
}

class PersonDetailJson {
    public final PersonJson person;
    public final List<BadgeJson> badges;
    public final List<BadgeJson> badgesInProgress;

    public PersonDetailJson(PersonJson person, List<BadgeJson> badges, List<BadgeJson> badgesInProgress) {
        this.person = person;
        this.badges = badges;
        this.badgesInProgress = badgesInProgress;
    }
}

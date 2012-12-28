package io.trygvis.esper.testing.web.resource;

import java.util.*;

public class PersonJson {
    public final UUID uuid;
    public final String name;
    public final List<BadgeJson> badges;
    public final List<BadgeJson> badgesInProgress;

    public PersonJson(UUID uuid, String name, List<BadgeJson> badges, List<BadgeJson> badgesInProgress) {
        this.uuid = uuid;
        this.name = name;
        this.badges = badges;
        this.badgesInProgress = badgesInProgress;
    }
}

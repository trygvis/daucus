package io.trygvis.esper.testing.web.resource;

import io.trygvis.esper.testing.core.badge.*;
import org.joda.time.*;

import java.util.*;

public class BadgeJson {
    public final UUID uuid;
    public final DateTime createdDate;
    public final String name;
    public final int level;

    public final Integer progress;
    public final Integer goal;

    /**
     * For completed badges.
     */
    public BadgeJson(UUID uuid, DateTime createdDate, String name, int level) {
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.name = name;
        this.level = level;
        this.progress = null;
        this.goal = null;
    }

    /**
     * For badges in progress.
     */
    public BadgeJson(String name, int level, int progress, int goal) {
        this.uuid = null;
        this.createdDate = null;
        this.name = name;
        this.level = level;
        this.progress = progress;
        this.goal = goal;
    }
}

class BadgeDetailJson {
    public final BadgeJson badge;
    public final PersonalBadge personalBadge;
    public final PersonJson person;

    BadgeDetailJson(BadgeJson badge, PersonalBadge personalBadge, PersonJson person) {
        this.badge = badge;
        this.personalBadge = personalBadge;
        this.person = person;
    }
}

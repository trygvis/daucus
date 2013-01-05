package io.trygvis.esper.testing.web.resource;

import org.joda.time.*;

public class BadgeJson {
    public final DateTime createdDate;
    public final String name;
    public final int level;

    public final Integer progress;
    public final Integer goal;

    /**
     * For completed badges.
     */
    public BadgeJson(DateTime createdDate, String name, int level) {
        this.createdDate = createdDate;
        this.name = name;
        this.level = level;
        this.progress = null;
        this.goal = null;
    }

    /**
     * For badges in progress.
     */
    public BadgeJson(DateTime createdDate, String name, int level, int progress, int goal) {
        this.createdDate = createdDate;
        this.name = name;
        this.level = level;
        this.progress = progress;
        this.goal = goal;
    }
}

class BadgeDetailJson {
    public final BadgeJson badge;
    public final PersonJson person;

    BadgeDetailJson(BadgeJson badge, PersonJson person) {
        this.badge = badge;
        this.person = person;
    }
}

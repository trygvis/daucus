package io.trygvis.esper.testing.web.resource;

public class BadgeJson {
    public final String name;
    public final int level;

    /**
     * Number of times this badge has been received.
     */
    public final int count;
    public final int progress;
    public final int goal;

    public BadgeJson(String name, int level, int count, int progress, int goal) {
        this.name = name;
        this.level = level;
        this.count = count;
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

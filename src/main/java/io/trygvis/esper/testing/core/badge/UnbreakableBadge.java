package io.trygvis.esper.testing.core.badge;

import io.trygvis.esper.testing.*;

import java.util.*;

class UnbreakableBadge extends PersonalBadge {
    // Configuration for this badge
    public static final int LEVEL_1_COUNT = 10;
    public static final int LEVEL_2_COUNT = 20;
    public static final int LEVEL_3_COUNT = 50;

    public final List<UUID> builds;

    @SuppressWarnings("UnusedDeclaration")
    private UnbreakableBadge() {
        super(null, 0);
        builds = null;
    }

    UnbreakableBadge(Uuid person, int level, List<UUID> builds) {
        super(person, level);
        this.builds = builds;
    }
}

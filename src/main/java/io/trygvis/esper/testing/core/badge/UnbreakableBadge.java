package io.trygvis.esper.testing.core.badge;

class UnbreakableBadge {
    // Configuration for this badge
    public static final int LEVEL_1_COUNT = 10;
    public static final int LEVEL_2_COUNT = 20;
    public static final int LEVEL_3_COUNT = 50;

    public final int level;

    UnbreakableBadge(int level) {
        this.level = level;
    }
}

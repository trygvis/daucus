package io.trygvis.esper.testing.core.badge;

import io.trygvis.esper.testing.core.db.PersonalBadgeDto.*;

public abstract class BadgeProgress {
    public final BadgeType type;

    protected BadgeProgress(BadgeType type) {
        this.type = type;
    }

    public abstract int progression();
    public abstract int goal();
    public abstract int progressingAgainstLevel();
}

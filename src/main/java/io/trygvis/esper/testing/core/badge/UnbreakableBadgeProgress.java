package io.trygvis.esper.testing.core.badge;

import static fj.P.*;
import fj.*;
import fj.data.*;
import static fj.data.Option.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.core.db.PersonBadgeDto.*;

import java.util.*;

public class UnbreakableBadgeProgress extends BadgeProgress {
    public final UUID person;
    public final int count;

    private UnbreakableBadgeProgress(UUID person, int count) {
        super(BadgeType.UNBREAKABLE);
        this.person = person;
        this.count = count;
    }

    @SuppressWarnings("UnusedDeclaration")
    private UnbreakableBadgeProgress() {
        super(BadgeType.UNBREAKABLE);
        person = null;
        count = -1;
    }

    public static UnbreakableBadgeProgress initial(UUID person) {
        return new UnbreakableBadgeProgress(person, 0);
    }

    public P2<UnbreakableBadgeProgress, Option<UnbreakableBadge>> onBuild(BuildDto build) {
        if (!build.success) {
            return p(initial(person), Option.<UnbreakableBadge>none());
        }

        int count = this.count + 1;

        if (count == UnbreakableBadge.LEVEL_3_COUNT) {
            return p(initial(person), some(new UnbreakableBadge(3)));
        }

        if (count == UnbreakableBadge.LEVEL_2_COUNT) {
            return p(new UnbreakableBadgeProgress(person, count), some(new UnbreakableBadge(2)));
        }

        if (count == UnbreakableBadge.LEVEL_1_COUNT) {
            return p(new UnbreakableBadgeProgress(person, count), some(new UnbreakableBadge(1)));
        }

        return p(new UnbreakableBadgeProgress(person, count), Option.<UnbreakableBadge>none());
    }

    public int progression() {
        return count;
    }

    public int goal() {
        if (count > UnbreakableBadge.LEVEL_2_COUNT) {
            return UnbreakableBadge.LEVEL_3_COUNT;
        }

        if (count > UnbreakableBadge.LEVEL_1_COUNT) {
            return UnbreakableBadge.LEVEL_2_COUNT;
        }

        return UnbreakableBadge.LEVEL_1_COUNT;
    }

    public int progressingAgainstLevel() {
        if (count > UnbreakableBadge.LEVEL_2_COUNT) {
            return 3;
        }

        if (count > UnbreakableBadge.LEVEL_1_COUNT) {
            return 2;
        }

        return 1;
    }

    public String toString() {
        return "UnbreakableBadgeProgress{person=" + person + ", count=" + count + '}';
    }
}

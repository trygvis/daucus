package io.trygvis.esper.testing.core.badge;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.core.db.*;

import java.util.*;

import static fj.P.p;
import static fj.data.Option.some;

class UnbreakableBadgeProgress {
    public final UUID person;
    public final int count;

    private UnbreakableBadgeProgress(UUID person, int count) {
        this.person = person;
        this.count = count;
    }

    @SuppressWarnings("UnusedDeclaration")
    private UnbreakableBadgeProgress() {
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

    @Override
    public String toString() {
        return "UnbreakableBadgeProgress{person=" + person + ", count=" + count + '}';
    }
}

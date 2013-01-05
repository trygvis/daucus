package io.trygvis.esper.testing.core.badge;

import static fj.P.*;
import fj.*;
import fj.data.*;
import static fj.data.Option.*;
import static java.util.Collections.singletonList;

import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.core.db.PersonalBadgeDto.*;

import java.util.*;
import java.util.List;

public class UnbreakableBadgeProgress extends BadgeProgress {
    public final Uuid person;
    public final List<UUID> builds;

    private UnbreakableBadgeProgress(Uuid person, List<UUID> builds) {
        super(BadgeType.UNBREAKABLE);
        this.person = person;
        this.builds = Collections.unmodifiableList(builds);
    }

    @SuppressWarnings("UnusedDeclaration")
    private UnbreakableBadgeProgress() {
        super(BadgeType.UNBREAKABLE);
        person = null;
        builds = null;
    }

    public static UnbreakableBadgeProgress initial(Uuid person) {
        return new UnbreakableBadgeProgress(person, Collections.<UUID>emptyList());
    }

    public P2<UnbreakableBadgeProgress, Option<UnbreakableBadge>> onBuild(BuildDto build) {
        if (!build.success) {
            return p(initial(person), Option.<UnbreakableBadge>none());
        }

        List<UUID> builds = new ArrayList<>(this.builds);
        builds.add(build.uuid);

        if (progression() == UnbreakableBadge.LEVEL_3_COUNT) {
            // You have to start from scratch now.
            builds = singletonList(build.uuid);
            return p(new UnbreakableBadgeProgress(person, builds), some(new UnbreakableBadge(person, 3, builds)));
        }

        if (progression() == UnbreakableBadge.LEVEL_2_COUNT) {
            return p(new UnbreakableBadgeProgress(person, builds), some(new UnbreakableBadge(person, 2, builds)));
        }

        if (progression() == UnbreakableBadge.LEVEL_1_COUNT) {
            return p(new UnbreakableBadgeProgress(person, builds), some(new UnbreakableBadge(person, 1, builds)));
        }

        return p(new UnbreakableBadgeProgress(person, builds), Option.<UnbreakableBadge>none());
    }

    public int progression() {
        return builds.size();
    }

    public int goal() {
        if (progression() > UnbreakableBadge.LEVEL_2_COUNT) {
            return UnbreakableBadge.LEVEL_3_COUNT;
        }

        if (progression() > UnbreakableBadge.LEVEL_1_COUNT) {
            return UnbreakableBadge.LEVEL_2_COUNT;
        }

        return UnbreakableBadge.LEVEL_1_COUNT;
    }

    public int progressingAgainstLevel() {
        if (progression() > UnbreakableBadge.LEVEL_2_COUNT) {
            return 3;
        }

        if (progression() > UnbreakableBadge.LEVEL_1_COUNT) {
            return 2;
        }

        return 1;
    }

    public String toString() {
        return "UnbreakableBadgeProgress{person=" + person + ", #builds=" + builds.size() + '}';
    }
}

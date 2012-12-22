package io.trygvis.esper.testing.core.badge;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.core.db.*;
import junit.framework.*;
import org.joda.time.*;

import java.util.*;
import java.util.List;

import static java.util.UUID.*;

public class UnbreakableBadgeProgressTest extends TestCase {
    UUID uuid = randomUUID();

    public void testBadge() {
        BuildDto build = new BuildDto(uuid, new DateTime(), new DateTime(), true, null);

        UUID person = randomUUID();

        UnbreakableBadgeProgress p = UnbreakableBadgeProgress.initial(person);

        List<UnbreakableBadge> badges = new ArrayList<>();

        for (int i = 0; i < 55; i++) {
            P2<UnbreakableBadgeProgress, Option<UnbreakableBadge>> p2 = p.onBuild(build);

            if (p2._2().isSome()) {
                badges.add(p2._2().some());
            }

            p = p2._1();
        }

        assertEquals(5, p.count);
        assertEquals(3, badges.size());
        assertEquals(1, badges.get(0).level);
        assertEquals(2, badges.get(1).level);
        assertEquals(3, badges.get(2).level);
    }
}

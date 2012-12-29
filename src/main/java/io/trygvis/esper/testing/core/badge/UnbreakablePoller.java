package io.trygvis.esper.testing.core.badge;

import com.jolbox.bonecp.*;
import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.util.sql.*;
import org.slf4j.*;

import java.sql.*;
import java.util.List;
import java.util.*;

import static io.trygvis.esper.testing.Config.*;
import static io.trygvis.esper.testing.core.db.PersonBadgeDto.BadgeType.*;

public class UnbreakablePoller implements TablePoller.NewRowCallback<BuildDto> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BadgeService badgeService = new BadgeService();

    public static void main(String[] args) throws Exception {
        String pollerName = "unbreakable";
        String tableName = "build";
        String columnNames = BuildDao.BUILD;
        SqlF<ResultSet, BuildDto> f = BuildDao.build;
        TablePoller.NewRowCallback<BuildDto> callback = new UnbreakablePoller();

        Config config = loadFromDisk("unbreakable-poller");

        BoneCPDataSource dataSource = config.createBoneCp();

        new TablePoller<>(pollerName, tableName, columnNames, Option.<String>none(), f, callback).
                testMode(true).
                work(dataSource);
    }

    public void process(Connection c, BuildDto build) throws SQLException {
        Daos daos = new Daos(c);

        List<UUID> persons = daos.buildDao.selectBuildParticipantByBuild(build.uuid);
        logger.info("Processing build={}, success={}, #persons={}", build.uuid, build.success, persons.size());

        for (UUID person : persons) {
            logger.info("person={}", person);

            SqlOption<PersonBadgeProgressDto> o = daos.personDao.selectBadgeProgress(person, UNBREAKABLE);

            if (o.isNone()) {
                UnbreakableBadgeProgress badge = UnbreakableBadgeProgress.initial(person);
                logger.info("New badge progress");
                String state = badgeService.serialize(badge);
                daos.personDao.insertBadgeProgress(person, UNBREAKABLE, state);
                continue;
            }

            UnbreakableBadgeProgress badge = badgeService.unbreakable(o.get());

            logger.info("Existing badge progress: count={}", badge.count);

            P2<UnbreakableBadgeProgress, Option<UnbreakableBadge>> p = badge.onBuild(build);

            badge = p._1();

            logger.info("New badge progress: count={}", badge.count);

            if (p._2().isSome()) {
                UnbreakableBadge b = p._2().some();

                logger.info("New unbreakable badge: person={}, level={}", person, b.level);

                SqlOption<PersonBadgeDto> option = daos.personDao.selectBadge(person, UNBREAKABLE, b.level);

                if (option.isNone()) {
                    daos.personDao.insertBadge(person, UNBREAKABLE, b.level, 1);
                } else {
                    daos.personDao.incrementBadgeCount(person, UNBREAKABLE, b.level);
                }
            }

            String state = badgeService.serialize(badge);

            daos.personDao.updateBadgeProgress(person, UNBREAKABLE, state);
        }
    }
}

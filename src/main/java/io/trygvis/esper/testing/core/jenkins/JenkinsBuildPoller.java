package io.trygvis.esper.testing.core.jenkins;

import com.jolbox.bonecp.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.jenkins.*;
import io.trygvis.esper.testing.util.sql.*;
import org.slf4j.*;

import java.sql.*;
import java.util.*;

import static fj.data.Option.*;
import static io.trygvis.esper.testing.Config.*;
import static io.trygvis.esper.testing.EntityRef.*;

public class JenkinsBuildPoller implements TablePoller.NewRowCallback<JenkinsBuildDto> {
    Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws Exception {
        String pollerName = "jenkins_build";
        String tableName = "jenkins_build";
        String columnNames = JenkinsDao.JENKINS_BUILD;
        SqlF<ResultSet, JenkinsBuildDto> f = JenkinsDao.jenkinsBuild;
        TablePoller.NewRowCallback<JenkinsBuildDto> callback = new JenkinsBuildPoller();

        Config config = loadFromDisk();

        BoneCPDataSource dataSource = config.createBoneCp();

        new TablePoller<>(pollerName, tableName, columnNames, some("array_length(users, 1) > 0"), f, callback).work(dataSource);
    }

    public void process(Connection c, JenkinsBuildDto jenkinsBuild) throws SQLException {
        Daos daos = new Daos(c);
        BuildDao buildDao = daos.buildDao;
        PersonDao personDao = daos.personDao;

        UUID uuid = buildDao.insertBuild(jenkinsBuild.timestamp, "SUCCESS".equals(jenkinsBuild.result), jenkinsRef(jenkinsBuild.uuid));

        int knownPersons = 0, unknownPersons = 0;

        for (UUID user : jenkinsBuild.users) {
            SqlOption<PersonDto> personO = personDao.selectPersonByJenkinsUuid(user);

            // This happens if no one has claimed the user id.
            if (personO.isNone()) {
                unknownPersons++;
                continue;
            }

            knownPersons++;

            UUID person = personO.get().uuid;
            logger.info("Created build participant, person={}", person);
            buildDao.insertBuildParticipant(uuid, person);
        }

        logger.info("Created build uuid={}, #participants={}, #knownPersons={}, #unknonwnPersons={}", uuid,
                jenkinsBuild.users.length, knownPersons, unknownPersons);
    }
}

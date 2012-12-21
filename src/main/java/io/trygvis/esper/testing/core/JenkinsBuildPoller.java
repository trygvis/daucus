package io.trygvis.esper.testing.core;

import com.jolbox.bonecp.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.jenkins.*;
import io.trygvis.esper.testing.sql.*;
import org.slf4j.*;

import java.sql.*;
import java.util.*;

import static fj.data.Option.some;
import static io.trygvis.esper.testing.Config.*;
import static io.trygvis.esper.testing.EntityRef.jenkinsRef;

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
        CoreDao coreDao = daos.coreDao;

        UUID uuid = coreDao.insertBuild(jenkinsBuild.timestamp, "SUCCESS".equals(jenkinsBuild.result), jenkinsRef(jenkinsBuild.uuid));
        logger.info("Created build uuid={}", uuid);

        for (UUID user : jenkinsBuild.users) {
            SqlOption<PersonDto> personO = coreDao.selectPersonByJenkinsUuid(user);

            // This happens if no one has claimed the user id.
            if(personO.isNone()) {
                continue;
            }

            UUID person = personO.get().uuid;
            logger.info("Created build participant, person={}", person);
            coreDao.insertBuildParticipant(uuid, person);
        }
    }
}

package io.trygvis.esper.testing.core.jenkins;

import com.jolbox.bonecp.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.jenkins.*;
import io.trygvis.esper.testing.jenkins.xml.*;
import io.trygvis.esper.testing.util.*;
import io.trygvis.esper.testing.util.sql.*;
import org.joda.time.*;
import org.slf4j.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import static fj.data.Option.*;
import static io.trygvis.esper.testing.Config.*;
import static io.trygvis.esper.testing.EntityRef.jenkinsBuildRef;

public class JenkinsBuildPoller implements TablePoller.NewRowCallback<JenkinsBuildDto> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final XmlParser xmlParser = new XmlParser();

    public static void main(String[] args) throws Exception {
        String pollerName = "jenkins_build";
        String tableName = "jenkins_build";
        String columnNames = JenkinsDao.JENKINS_BUILD;
        SqlF<ResultSet, JenkinsBuildDto> f = JenkinsDao.jenkinsBuild;
        TablePoller.NewRowCallback<JenkinsBuildDto> callback = new JenkinsBuildPoller();

        Config config = loadFromDisk("jenkins-build-poller");

        BoneCPDataSource dataSource = config.createBoneCp();

        new TablePoller<>(pollerName, tableName, columnNames, some("array_length(users, 1) > 0"), f, callback).work(dataSource);
    }

    public void process(Connection c, final JenkinsBuildDto jenkinsBuild) throws SQLException {
        Daos daos = new Daos(c);
        final BuildDao buildDao = daos.buildDao;

        JenkinsJobDto jobDto = daos.jenkinsDao.selectJob(jenkinsBuild.job).get();

        SqlOption<InputStream> jobXmlFileO = daos.fileDao.load(jobDto.file);

        if (jobXmlFileO.isNone()) {
            logger.warn("Job xml file unavailable: File.uuid={}", jobDto.file);
            return;
        }

        Option<JenkinsJobXml> jobXmlO = xmlParser.parseDocument.f(jobXmlFileO.get()).
                bind(JenkinsJobXml.parse);

        if (jobXmlO.isNone()) {
            logger.warn("Could not parse job file: File.uuid={}", jobDto.file);
            return;
        }

        JenkinsJobXml jobXml = jobXmlO.some();

        switch (jobXml.type) {
            case MAVEN_MODULE:
                logger.info("Skipping maven module, Job.uuid={}", jobDto.uuid);
                return;
        }

        SqlOption<InputStream> file = daos.fileDao.load(jenkinsBuild.file);

        if (file.isNone()) {
            logger.warn("Build file unavailable: " + jenkinsBuild.file);
            return;
        }

        Option<JenkinsBuildXml> o = file.toFj().
                bind(xmlParser.parseDocument).
                bind(JenkinsBuildXml.parse);

        if (o.isNone()) {
            logger.warn("Unable to parse and process xml: " + jenkinsBuild.file);
            return;
        }

        final JenkinsBuildXml jenkinsBuildXml = o.some();

        SqlOption<UUID> uuidBuildO = buildDao.findBuildByReference(jenkinsBuild.toRef());

        UUID uuidBuild = uuidBuildO.getOrElse(new SqlP0<UUID>() {
            public UUID apply() throws SQLException {
                return buildDao.insertBuild(
                        new DateTime(jenkinsBuildXml.timestamp),
                        "SUCCESS".equals(jenkinsBuildXml.result.orSome("")),
                        jenkinsBuildRef(jenkinsBuild.uuid));
            }
        });

        int knownPersons = 0, unknownPersons = 0;

        for (UUID user : jenkinsBuild.users) {
            SqlOption<PersonDto> personO = daos.personDao.selectPersonByJenkinsUuid(user);

            // This happens if no one has claimed the user id.
            if (personO.isNone()) {
                logger.info("unknown person: " + user);
                unknownPersons++;
                continue;
            }

            knownPersons++;

            Uuid person = personO.get().uuid;
            logger.info("Created build participant, person={}", person);
            buildDao.insertBuildParticipant(uuidBuild, person);
        }

        logger.info("Created build uuid={}, #participants={}, #knownPersons={}, #unknonwnPersons={}", uuidBuild,
                jenkinsBuild.users.length, knownPersons, unknownPersons);
    }
}

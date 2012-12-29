package io.trygvis.esper.testing.jenkins;

import com.jolbox.bonecp.*;
import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.util.*;
import org.apache.abdera.*;
import org.codehaus.httpcache4j.cache.*;

import java.sql.*;
import java.util.List;

import static io.trygvis.esper.testing.jenkins.JenkinsClient.*;
import static io.trygvis.esper.testing.jenkins.JenkinsDao.*;

public class SetJobTypeApp {
    public static void main(String[] args) throws Exception {
        final Config config = Config.loadFromDisk("set-job-type");
        final BoneCPDataSource boneCp = config.createBoneCp();
        HTTPCache httpCache = HttpClient.createHttpCache(config);
        Abdera abdera = config.createAbdera();
        final JenkinsClient jenkinsClient = new JenkinsClient(httpCache, abdera);

        try (Connection c = boneCp.getConnection()) {

            PreparedStatement s2 = c.prepareStatement("UPDATE jenkins_job SET job_type=? WHERE uuid=?");

            PreparedStatement s = c.prepareStatement("SELECT " + JenkinsDao.JENKINS_JOB + " FROM jenkins_job WHERE job_type IS NULL");

            List<JenkinsJobDto> jobs = Util.toList(s, jenkinsJob);

            System.out.println("jobs.size() = " + jobs.size());

            for (JenkinsJobDto jobDto : jobs) {
                System.out.println("job = " + jobDto.url);

                Option<P2<JenkinsJobXml,byte[]>> xmlOption = jenkinsClient.fetchJob(apiXml(jobDto.url));

                if(xmlOption.isNone()) {
                    System.out.println("None");
                    continue;
                }

                JenkinsJobXml jobXml = xmlOption.some()._1();

                System.out.println("jobXml.type = " + jobXml.type);

                s2.setString(1, jobXml.type.name());
                s2.setString(2, jobDto.uuid.toString());
                s2.executeUpdate();
            }

            c.commit();
        }
    }
}

package io.trygvis.esper.testing.jenkins;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.abdera.Abdera;
import org.codehaus.httpcache4j.cache.HTTPCache;

import com.jolbox.bonecp.BoneCPDataSource;

import fj.data.Option;
import io.trygvis.esper.testing.Config;
import io.trygvis.esper.testing.util.HttpClient;

import static io.trygvis.esper.testing.jenkins.JenkinsClient.apiXml;

public class SetJobTypeApp {
    public static void main(String[] args) throws Exception {
        final Config config = Config.loadFromDisk();
        final BoneCPDataSource boneCp = config.createBoneCp();
        HTTPCache httpCache = HttpClient.createHttpCache(config);
        Abdera abdera = config.createAbdera();
        final JenkinsClient jenkinsClient = new JenkinsClient(httpCache, abdera);

        try (Connection c = boneCp.getConnection()) {

            PreparedStatement s2 = c.prepareStatement("UPDATE jenkins_job SET job_type=? WHERE uuid=?");

            PreparedStatement s = c.prepareStatement("SELECT " + JenkinsDao.JENKINS_JOB + " FROM jenkins_job WHERE job_type IS NULL");
            ResultSet rs = s.executeQuery();

            JenkinsDao dao = new JenkinsDao(c);

            List<JenkinsJobDto> jobs = dao.toJobList(rs);

            System.out.println("jobs.size() = " + jobs.size());

            for (JenkinsJobDto jobDto : jobs) {
                System.out.println("job = " + jobDto.url);

                Option<JenkinsJobXml> xmlOption = jenkinsClient.fetchJob(apiXml(jobDto.url));

                if(xmlOption.isNone()) {
                    System.out.println("None");
                    continue;
                }

                JenkinsJobXml jobXml = xmlOption.some();

                System.out.println("jobXml.type = " + jobXml.type);

                s2.setString(1, jobXml.type.name());
                s2.setString(2, jobDto.uuid.toString());
                s2.executeUpdate();
            }

            c.commit();
        }
    }
}

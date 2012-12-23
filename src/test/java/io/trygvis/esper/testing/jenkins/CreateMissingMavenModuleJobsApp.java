package io.trygvis.esper.testing.jenkins;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import fj.*;
import io.trygvis.esper.testing.core.db.*;
import org.apache.abdera.Abdera;
import org.codehaus.httpcache4j.cache.HTTPCache;

import com.jolbox.bonecp.BoneCPDataSource;

import fj.data.Option;
import io.trygvis.esper.testing.Config;
import io.trygvis.esper.testing.util.HttpClient;

import static io.trygvis.esper.testing.jenkins.JenkinsClient.apiXml;

public class CreateMissingMavenModuleJobsApp {
    public static void main(String[] args) throws Exception {
        final Config config = Config.loadFromDisk();
        final BoneCPDataSource boneCp = config.createBoneCp();
        HTTPCache httpCache = HttpClient.createHttpCache(config);
        Abdera abdera = config.createAbdera();
        final JenkinsClient client = new JenkinsClient(httpCache, abdera);

        try (Connection c = boneCp.getConnection()) {

//            PreparedStatement s2 = c.prepareStatement("UPDATE jenkins_job SET job_type=? WHERE uuid=?");

            PreparedStatement s = c.prepareStatement("SELECT " + JenkinsDao.JENKINS_JOB + " FROM jenkins_job WHERE job_type='MAVEN_MODULE'");
            ResultSet rs = s.executeQuery();

            JenkinsDao dao = new JenkinsDao(c);
            FileDao fileDao = new FileDao(c);

            List<JenkinsJobDto> jobs = dao.toJobList(rs);

            System.out.println("jobs.size() = " + jobs.size());

            for (JenkinsJobDto jobDto : jobs) {
                System.out.println("job = " + jobDto.url);

                String u = JenkinsServerActor.extrapolateMavenModuleFromMavenModuleSetUrl(jobDto.url.toASCIIString());

                System.out.println("u = " + u);

                URI url = URI.create(u);

                URI uri = apiXml(url);

                Option<P2<JenkinsJobXml,byte[]>> xmlOption = client.fetchJob(uri);

                if(xmlOption.isNone()) {
                    System.out.println("None");
                    continue;
                }

                JenkinsJobXml jobXml = xmlOption.some()._1();

                if(dao.selectJobByUrl(jobXml.url).isSome()) {
                    System.out.println("Duplicate");
                    continue;
                }

                UUID file = fileDao.store(uri, "application/xml", xmlOption.some()._2());

                UUID uuid = dao.insertJob(jobDto.server, file, url, jobXml.type, jobXml.displayName);

                System.out.println("New job: " + uuid);
            }

            c.commit();
        }
    }
}

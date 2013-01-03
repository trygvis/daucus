package io.trygvis.esper.testing.jenkins;

import com.jolbox.bonecp.*;
import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.jenkins.xml.JenkinsJobXml;
import io.trygvis.esper.testing.util.*;
import org.apache.abdera.*;
import org.codehaus.httpcache4j.cache.*;

import java.net.*;
import java.sql.*;
import java.util.List;
import java.util.*;

import static io.trygvis.esper.testing.jenkins.JenkinsClient.*;

public class CreateMissingMavenModuleJobsApp {
    public static void main(String[] args) throws Exception {
        final Config config = Config.loadFromDisk("create-missing-maven-module");
        final BoneCPDataSource boneCp = config.createBoneCp();
        HTTPCache httpCache = HttpClient.createHttpCache(config);
        Abdera abdera = config.createAbdera();
        final JenkinsClient client = new JenkinsClient(httpCache, abdera);

        try (Connection c = boneCp.getConnection()) {

//            PreparedStatement s2 = c.prepareStatement("UPDATE jenkins_job SET job_type=? WHERE uuid=?");

            PreparedStatement s = c.prepareStatement("SELECT " + JenkinsDao.JENKINS_JOB + " FROM jenkins_job WHERE job_type='MAVEN_MODULE'");

            JenkinsDao dao = new JenkinsDao(c);
            FileDao fileDao = new FileDao(c);

            List<JenkinsJobDto> jobs = Util.toList(s, JenkinsDao.jenkinsJob);

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

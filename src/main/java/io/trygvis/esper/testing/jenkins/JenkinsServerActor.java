package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import io.trygvis.esper.testing.object.*;
import org.slf4j.*;

import java.net.*;
import java.sql.*;
import java.util.List;
import java.util.*;

import static io.trygvis.esper.testing.jenkins.JenkinsClient.*;
import static java.lang.System.*;

public class JenkinsServerActor implements TransactionalActor {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsServerActor.class);
    private final JenkinsClient client;
    public final JenkinsServerDto server;

    public JenkinsServerActor(JenkinsClient client, JenkinsServerDto server) {
        this.client = client;
        this.server = server;
    }

    public void act(Connection c) throws Exception {
        long start = currentTimeMillis();

        JenkinsDao dao = new JenkinsDao(c);
        Option<List<JenkinsEntryXml>> option = client.fetchRss(URI.create(server.url.toASCIIString() + "/rssAll"));

        if (option.isNone()) {
            return;
        }

        List<JenkinsEntryXml> list = option.some();

        logger.info("Got " + list.size() + " entries.");

        int i = 0;

        for (JenkinsEntryXml entry : list) {
            Option<JenkinsBuildDto> o = dao.selectBuildByEntryId(entry.id);

            if (o.isSome()) {
                logger.debug("Old build: " + entry.id);
                continue;
            }

            logger.info("New build: " + entry.id + ", fetching info");

            Option<JenkinsBuildXml> buildXmlOption = client.fetchBuild(apiXml(entry.url));

            if (buildXmlOption.isNone()) {
                continue;
            }

            JenkinsBuildXml build = buildXmlOption.some();

            URI jobUrl = extrapolateJobUrlFromBuildUrl(build.url.toASCIIString());

            Option<JenkinsJobDto> jobDtoOption = dao.selectJobByUrl(jobUrl);

            UUID job;

            if (jobDtoOption.isSome()) {
                job = jobDtoOption.some().uuid;
            } else {
                logger.info("New job: " + jobUrl + ", fetching info");

                Option<JenkinsJobXml> jobXmlOption = client.fetchJob(apiXml(jobUrl));

                if (jobXmlOption.isNone()) {
                    continue;
                }

                JenkinsJobXml xml = jobXmlOption.some();

                job = dao.insertJob(server.uuid, xml.url, xml.type, xml.displayName);

                logger.info("New job: " + xml.displayName.orSome(xml.url.toASCIIString()) + ", uuid=" + job);
            }

            i++;

            UUID uuid = dao.insertBuild(
                    job,
                    entry.id,
                    build.url,
                    build.result,
                    build.number,
                    build.duration,
                    build.timestamp);

            logger.info("Build inserted: " + uuid + ", item #" + i + "/" + list.size());
        }

        long end = currentTimeMillis();

        logger.info("Inserted " + i + " of " + list.size() + " builds in " + (end - start) + "ms.");
    }

    /**
     * This sucks, a build should really include the URL to the job.
     */
    public static URI extrapolateJobUrlFromBuildUrl(String u) {
        if (!u.matches(".*/[0-9]*/")) {
            throw new RuntimeException("Not a valid build url: " + u);
        }

        u = u.substring(0, u.lastIndexOf("/"));
        u = u.substring(0, u.lastIndexOf("/") + 1);

        return URI.create(u);
    }

    public static String extrapolateMavenModuleFromMavenModuleSetUrl(String u) {
        int i = u.lastIndexOf("/");
        if (i == -1) {
            throw new RuntimeException("Illegal URL");
        }
        u = u.substring(0, i);
        i = u.lastIndexOf("/");
        if (i == -1) {
            throw new RuntimeException("Illegal URL");
        }
        return u.substring(0, i + 1);
    }
}

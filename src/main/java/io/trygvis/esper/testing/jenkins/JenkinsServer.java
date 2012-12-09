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

public class JenkinsServer implements TransactionalActor {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsServer.class);
    private final JenkinsClient client;
    public final JenkinsServerDto server;

    public JenkinsServer(JenkinsClient client, JenkinsServerDto server) {
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
                logger.debug("Old event: " + entry.id);
                continue;
            }

            logger.info("New build: " + entry.id + ", fetching info");

            Option<JenkinsBuildXml> buildXmlOption = client.fetchBuild(apiXml(entry.url));

            if (buildXmlOption.isNone()) {
                continue;
            }

            JenkinsBuildXml build = buildXmlOption.some();

            URI jobUrl = createJobUrl(build.url.toASCIIString());

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

                job = dao.insertJob(server.uuid, xml.url, xml.displayName);

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

            logger.info("Build inserted: " + uuid + ", i=" + i);
        }

        long end = currentTimeMillis();

        logger.info("Inserted " + i + " new builds in " + (end - start) + "ms.");
    }

    /**
     * This sucks, a build should really include the URL to the job.
     */
    public static URI createJobUrl(String u) {
        if (u.matches(".*/[-_a-zA-Z]*=.*/[0-9]*/")) {
            u = u.substring(0, u.lastIndexOf("/"));
            u = u.substring(0, u.lastIndexOf("/"));
            u = u.substring(0, u.lastIndexOf("/") + 1);

            return URI.create(u);
        }

        if (u.matches(".*/[.-_a-zA-Z]*\\$.*/[0-9]*/")) {
            u = u.substring(0, u.lastIndexOf("/"));
            u = u.substring(0, u.lastIndexOf("/"));
            u = u.substring(0, u.lastIndexOf("/") + 1);

            return URI.create(u);
        }

        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        int i = u.lastIndexOf("/");
        u = u.substring(0, i);

        return URI.create(u + "/");
    }
}

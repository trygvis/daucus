package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.jenkins.xml.*;
import io.trygvis.esper.testing.util.*;
import io.trygvis.esper.testing.util.object.*;
import io.trygvis.esper.testing.util.sql.*;
import org.slf4j.*;

import java.net.*;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.*;
import java.util.Set;

import static io.trygvis.esper.testing.jenkins.JenkinsClient.*;
import static java.lang.System.*;

public class JenkinsServerActor implements TransactionalActor {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsServerActor.class);
    private static final XmlParser xmlParser = new XmlParser();
    private final JenkinsClient client;
    public final JenkinsServerDto server;

    public JenkinsServerActor(JenkinsClient client, JenkinsServerDto server) {
        this.client = client;
        this.server = server;
    }

    public void act(Connection c) throws Exception {
        long start = currentTimeMillis();

        JenkinsDao dao = new JenkinsDao(c);
        FileDao fileDao = new FileDao(c);

        URI rssUrl = URI.create(server.url.toASCIIString() + "/rssAll");
        Option<P2<List<JenkinsEntryXml>, byte[]>> option = client.fetchRss(rssUrl);

        if (option.isNone()) {
            return;
        }

        fileDao.store(rssUrl, "application/xml", option.some()._2());

        List<JenkinsEntryXml> list = option.some()._1();

        logger.info("Got " + list.size() + " entries.");

        // Process the list from the oldest first.
        Collections.reverse(list);

        int i = 0;

        Map<String, UUID> authors = new HashMap<>();

        for (JenkinsEntryXml entry : list) {
            SqlOption<JenkinsBuildDto> o = dao.selectBuildByEntryId(entry.id);

            if (o.isSome()) {
//                logger.debug("Old build: " + entry.id);
                continue;
            }

            logger.debug("Build: " + entry.id + ", fetching info");

            URI buildUrl = apiXml(entry.url);

            Option<P2<JenkinsBuildXml, byte[]>> buildXmlOption = client.fetchBuild(buildUrl);

            if (buildXmlOption.isNone()) {
                continue;
            }

            JenkinsBuildXml build = buildXmlOption.some()._1();

            if (build.result.isNone()) {
                logger.debug("Not done building, <result> is not available.");
                continue;
            }

            UUID buildXmlFile = fileDao.store(buildUrl, "application/xml", buildXmlOption.some()._2());

            // -----------------------------------------------------------------------
            // Users
            // -----------------------------------------------------------------------

            Set<UUID> users = new HashSet<>();

            if (build.changeSet.isSome()) {
                JenkinsBuildXml.ChangeSetXml changeSetXml = build.changeSet.some();

                for (JenkinsBuildXml.ChangeSetItemXml item : changeSetXml.items) {
                    if (item.author.isNone()) {
                        continue;
                    }

                    String url = item.author.some().absoluteUrl;

                    UUID uuid = authors.get(url);

                    if (uuid == null) {
                        SqlOption<JenkinsUserDto> userO = dao.selectUserByAbsoluteUrl(server.uuid, url);
                        if (userO.isNone()) {
                            logger.info("New user: {}", url);
                            uuid = dao.insertUser(server.uuid, url);
                        } else {
                            uuid = userO.get().uuid;
                        }

                        authors.put(url, uuid);
                    }

                    users.add(uuid);
                }
            }

            // -----------------------------------------------------------------------
            // Job
            // -----------------------------------------------------------------------

            URI jobUrl = extrapolateJobUrlFromBuildUrl(build.url.toASCIIString());

            SqlOption<JenkinsJobDto> jobDtoOption = dao.selectJobByUrl(jobUrl);

            JenkinsJobDto job;

            if (jobDtoOption.isSome()) {
                job = jobDtoOption.get();

                logger.info("New build for job '{}'/{}", job.displayName(), job.uuid);
            } else {
                logger.info("New job: {}, fetching info", jobUrl);

                URI uri = apiXml(jobUrl);

                Option<P2<JenkinsJobXml, byte[]>> jobXmlOption = client.fetchJob(uri);

                if (jobXmlOption.isNone()) {
                    continue;
                }

                UUID jobXmlFile = fileDao.store(uri, "application/xml", jobXmlOption.some()._2());

                JenkinsJobXml xml = jobXmlOption.some()._1();

                UUID uuid = dao.insertJob(server.uuid, jobXmlFile, xml.url, xml.type, xml.displayName);

                job = dao.selectJob(uuid).get();

                logger.info("New job: '{}'/{}", xml.displayName.orSome(xml.url.toASCIIString()), uuid);
            }

            i++;

            checkForMissingBuilds(dao, fileDao, build, job);

            UUID uuid = dao.insertBuild(
                    job.uuid,
                    buildXmlFile,
                    entry.id,
                    build.url,
                    users.toArray(new UUID[users.size()]));

            logger.info("Build inserted: {}, name={}, number={}, #users={} item #{}/{}", uuid, job.displayName(), build.number, users.size(), i, list.size());
        }

        long end = currentTimeMillis();

        logger.info("Inserted " + i + " of " + list.size() + " builds in " + (end - start) + "ms.");
    }

    private void checkForMissingBuilds(JenkinsDao dao, FileDao fileDao, JenkinsBuildXml build, JenkinsJobDto job) throws SQLException {
        if (build.number <= 1) {
            return;
        }

        List<JenkinsBuildDto> builds = dao.selectBuildByJob(job.uuid, PageRequest.one());

        if (builds.isEmpty()) {
            return;
        }

        JenkinsBuildDto previousJob = builds.get(0);

        Option<JenkinsBuildXml> previousXmlO = fileDao.load(previousJob.file).toFj().
                bind(xmlParser.parseDocument).
                bind(JenkinsBuildXml.parse);

        if (previousXmlO.isNone()) {
            logger.warn("Unable to load/parse XML file from previous build, file={}", previousJob.file);
            return;
        }

        JenkinsBuildXml previousBuildXml = previousXmlO.some();

        if (build.number != previousBuildXml.number + 1) {
            logger.warn("MISSED BUILD. The build number for job={} ({}) was {}, but the previous build was {}",
                    job.displayName(), job.uuid, build.number, previousBuildXml.number);
        }
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

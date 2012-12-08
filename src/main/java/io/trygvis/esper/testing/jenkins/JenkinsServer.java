package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import io.trygvis.esper.testing.object.*;
import org.slf4j.*;

import java.net.*;
import java.sql.*;
import java.util.List;
import java.util.*;

import static io.trygvis.esper.testing.jenkins.JenkinsClient.*;

public class JenkinsServer implements TransactionalActor {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsServer.class);
    private final JenkinsClient client;
    public final URI uri;

    public JenkinsServer(JenkinsClient client, URI uri) {
        this.client = client;
        this.uri = uri;
    }

    public void act(Connection c) throws Exception {
        JenkinsDao dao = new JenkinsDao(c);
        Option<List<JenkinsEntryXml>> option = client.fetchRss(URI.create(uri.toASCIIString() + "/rssAll"));

        if(option.isNone()) {
            return;
        }

        List<JenkinsEntryXml> list = option.some();

        logger.info("Got " + list.size() + " entries.");

        int i = 0;

        for (JenkinsEntryXml entry : list) {
            Option<JenkinsBuildDto> o = dao.selectBuildByEntryId(entry.id);

            if(o.isSome()) {
                logger.info("Old event: " + entry.id);
                continue;
            }

            logger.info("New event: " + entry.id + ", fetching build info");

            i++;

            Option<JenkinsBuildXml> o2 = client.fetchBuild(URI.create(entry.uri.toASCIIString() + "/api/xml"));

            if(o2.isNone()) {
                continue;
            }

            JenkinsBuildXml build = o2.some();

            UUID uuid = dao.insertBuild(entry.id, build.uri, build.result, build.number, build.duration, build.timestamp);

            logger.info("Build inserted: " + uuid + ", i=" + i);

//            if(i == 1) {
//                break;
//            }
        }

        logger.info("Inserted " + i + " new events.");
    }
}

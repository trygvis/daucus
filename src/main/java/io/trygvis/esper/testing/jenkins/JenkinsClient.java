package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.jenkins.JenkinsJobXml.*;
import io.trygvis.esper.testing.util.*;
import org.apache.abdera.*;
import org.apache.abdera.model.*;
import org.apache.abdera.parser.*;
import org.codehaus.httpcache4j.cache.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.DateTime;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import static fj.data.Option.*;
import static io.trygvis.esper.testing.util.HttpClient.inputStreamOnly;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang.StringUtils.*;

public class JenkinsClient {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsClient.class);
    private final XmlHttpClient xmlHttpClient;
    private final HttpClient<List<JenkinsEntryXml>> jenkinsEntryXmlClient;
    private final Parser parser;

    public JenkinsClient(HTTPCache http, Abdera abdera) {
        this.xmlHttpClient = new XmlHttpClient(http);
        this.parser = abdera.getParser();

        jenkinsEntryXmlClient = new HttpClient<>(http, inputStreamOnly(new F<InputStream, Option<List<JenkinsEntryXml>>>() {
            public Option<List<JenkinsEntryXml>> f(InputStream inputStream) {
                long start = currentTimeMillis();
                Feed feed = (Feed) parser.parse(inputStream).getRoot();
                long end = currentTimeMillis();
                logger.info("Parsed document in " + (end - start) + "ms.");

                List<JenkinsEntryXml> list = new ArrayList<>();

                for (Entry entry : feed.getEntries()) {
                    try {
                        list.add(new JenkinsEntryXml(entry.getIdElement().getText(), new DateTime(entry.getPublished().getTime()), entry.getAlternateLinkResolvedHref().toURI()));
                    } catch (URISyntaxException ignore) {
                    }
                }

                long end2 = currentTimeMillis();

                logger.info("Converted document to JenkinsEntryXml in " + (end2 - end) + "ms.");

                return some(list);
            }
        }));
    }

    public static URI apiXml(URI url) {
        String u = url.toASCIIString();

        if(u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }

        return URI.create(u + "/api/xml");
    }

    public Option<List<JenkinsEntryXml>> fetchRss(URI url) {
        return jenkinsEntryXmlClient.fetch(url);
    }

    public JenkinsXml fetchJobs(URI url) {
        Option<Document> d = xmlHttpClient.fetch(url);

        if (d.isNone()) {
            Option<String> n = Option.none();

            return new JenkinsXml(n, n, n, Collections.<JenkinsXml.JobXml>emptyList());
        }

        Element root = d.some().getRootElement();

        List<JenkinsXml.JobXml> jobs = new ArrayList<>();
        for (Element job : root.getChildren("job")) {
            String name = trimToNull(job.getChildText("name"));
            String u = trimToNull(job.getChildText("url"));
            String color = trimToNull(job.getChildText("color"));

            if (name == null || u == null || color == null) {
                continue;
            }

            jobs.add(new JenkinsXml.JobXml(name, u, color));
        }

        return new JenkinsXml(
            Option.fromNull(root.getChildText("nodeName")),
            Option.fromNull(root.getChildText("nodeDescription")),
            Option.fromNull(root.getChildText("description")), jobs);
    }

    public Option<JenkinsJobXml> fetchJob(URI url) {
        Option<Document> d = xmlHttpClient.fetch(url);

        if (d.isNone()) {
            return Option.none();
        }

        Element root = d.some().getRootElement();

        String name = root.getName();

        switch (name) {
            case "freeStyleProject":
                return some(JenkinsJobXml.parse(url, JenkinsJobType.FREE_STYLE, root));
            case "mavenModuleSet":
                return some(JenkinsJobXml.parse(url, JenkinsJobType.MAVEN_MODULE_SET, root));
            case "mavenModule":
                return some(JenkinsJobXml.parse(url, JenkinsJobType.MAVEN_MODULE, root));
            case "matrixProject":
                return some(JenkinsJobXml.parse(url, JenkinsJobType.MATRIX, root));
            default:
                logger.warn("Unknown project type: " + name);
                return Option.none();
        }
    }

    public Option<JenkinsBuildXml> fetchBuild(URI url) {
        Option<Document> d = xmlHttpClient.fetch(url);

        if (d.isNone()) {
            return Option.none();
        }

        Element root = d.some().getRootElement();

        String name = root.getName();

        switch (name) {
            // I don't know the different between a "matrix build" and "matrix run"
            case "matrixBuild":
            case "matrixRun":
            case "mavenModuleSetBuild":
            case "mavenBuild":
            case "freeStyleBuild":
                return JenkinsBuildXml.parse(root);
            default:
                logger.warn("Unknown build type: " + name);
                return Option.none();
        }
    }

}

class JenkinsEntryXml {
    public final String id;
    public final DateTime timestamp;
    public final URI url;

    JenkinsEntryXml(String id, DateTime timestamp, URI url) {
        this.id = id;
        this.timestamp = timestamp;
        this.url = url;
    }
}

class JenkinsXml {
    public final Option<String> nodeName;
    public final Option<String> nodeDescription;
    public final Option<String> description;
    public final List<JobXml> jobs;

    JenkinsXml(Option<String> nodeName, Option<String> nodeDescription, Option<String> description, List<JobXml> jobs) {
        this.nodeName = nodeName;
        this.nodeDescription = nodeDescription;
        this.description = description;
        this.jobs = jobs;
    }

    public static class JobXml {
        public final String name;
        public final String url;
        public final String color;

        JobXml(String name, String url, String color) {
            this.name = name;
            this.url = url;
            this.color = color;
        }
    }
}

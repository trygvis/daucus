package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
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
import static io.trygvis.esper.testing.Util.*;
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

            return new JenkinsXml(n, n, n, Collections.<JenkinsJobEntryXml>emptyList());
        }

        Element root = d.some().getRootElement();

        List<JenkinsJobEntryXml> jobs = new ArrayList<>();
        for (Element job : root.getChildren("job")) {
            String name = trimToNull(job.getChildText("name"));
            String u = trimToNull(job.getChildText("url"));
            String color = trimToNull(job.getChildText("color"));

            if (name == null || u == null || color == null) {
                continue;
            }

            jobs.add(new JenkinsJobEntryXml(name, u, color));
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
                return some(JenkinsJobXml.parse(url, JenkinsJobType.MAVEN, root));
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

    public static class JenkinsBuildXml {

        public final URI url;
        public final int number;
        public final String result;
        public final int duration;
        public final long timestamp;

        JenkinsBuildXml(URI url, int number, String result, int duration, long timestamp) {
            this.url = url;
            this.number = number;
            this.result = result;
            this.duration = duration;
            this.timestamp = timestamp;
        }

        public static Option<JenkinsBuildXml> parse(Element root) {
            Option<URI> url = childText(root, "url").bind(Util.parseUri);
            Option<Integer> number = childText(root, "number").bind(Util.parseInt);
            Option<String> result = childText(root, "result");
            Option<Integer> duration = childText(root, "duration").bind(Util.parseInt);
            Option<Long> timestamp = childText(root, "timestamp").bind(Util.parseLong);

            if(url.isNone()) {
                logger.warn("Missing required field: <url>");
                return none();
            }
            if(number.isNone()) {
                logger.warn("Missing required field: <number>");
                return none();
            }
            if(result.isNone()) {
                logger.warn("Missing required field: <result>");
                return none();
            }
            if(duration.isNone()) {
                logger.warn("Missing required field: <duration>");
                return none();
            }
            if(timestamp.isNone()) {
                logger.warn("Missing required field: <timestamp>");
                return none();
            }

            return some(new JenkinsBuildXml(url.some(), number.some(), result.some(), duration.some(), timestamp.some()));
        }
    }}

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
    public final List<JenkinsJobEntryXml> jobs;

    JenkinsXml(Option<String> nodeName, Option<String> nodeDescription, Option<String> description, List<JenkinsJobEntryXml> jobs) {
        this.nodeName = nodeName;
        this.nodeDescription = nodeDescription;
        this.description = description;
        this.jobs = jobs;
    }
}

class JenkinsJobEntryXml {
    public final String name;
    public final String url;
    public final String color;

    JenkinsJobEntryXml(String name, String url, String color) {
        this.name = name;
        this.url = url;
        this.color = color;
    }
}

class JenkinsJobXml {
    enum JenkinsJobType {
        MAVEN, FREE_STYLE, MATRIX
    }

    public final JenkinsJobType type;
    public final Option<String> description;
    public final Option<String> displayName;
    public final Option<String> name;
    public final URI url;
    public final Option<String> color;
    public final boolean buildable;
    public final Option<BuildXml> lastBuild;
    public final Option<BuildXml> lastCompletedBuild;
    public final Option<BuildXml> lastFailedBuild;
    public final Option<BuildXml> lastSuccessfulBuild;
    public final Option<BuildXml> lastUnsuccessfulBuild;

    protected JenkinsJobXml(JenkinsJobType type, Option<String> description, Option<String> displayName,
                            Option<String> name, URI url, Option<String> color, boolean buildable,
                            Option<BuildXml> lastBuild, Option<BuildXml> lastCompletedBuild,
                            Option<BuildXml> lastFailedBuild, Option<BuildXml> lastSuccessfulBuild,
                            Option<BuildXml> lastUnsuccessfulBuild) {
        this.type = type;
        this.description = description;
        this.displayName = displayName;
        this.name = name;
        this.url = url;
        this.color = color;
        this.buildable = buildable;
        this.lastBuild = lastBuild;
        this.lastCompletedBuild = lastCompletedBuild;
        this.lastFailedBuild = lastFailedBuild;
        this.lastSuccessfulBuild = lastSuccessfulBuild;
        this.lastUnsuccessfulBuild = lastUnsuccessfulBuild;
    }

    static class BuildXml {
        public final int number;
        public final URI url;
        public static F<Element, Option<BuildXml>> buildXml = new F<Element, Option<BuildXml>>() {
            public Option<BuildXml> f(Element element) {
                Option<Integer> number = childText(element, "number").bind(Util.parseInt);
                Option<URI> url = childText(element, "url").bind(Util.parseUri);

                if (number.isNone() || url.isNone()) {
                    return Option.none();
                }

                return some(new BuildXml(number.some(), url.some()));
            }
        };

        BuildXml(int number, URI url) {
            this.number = number;
            this.url = url;
        }
    }

    public static JenkinsJobXml parse(URI url, JenkinsJobType type, Element root) {
        return new JenkinsJobXml(type,
            childText(root, "description"),
            childText(root, "displayName"),
            childText(root, "name"),
            childText(root, "url").bind(Util.parseUri).orSome(url),
            childText(root, "color"),
            childText(root, "buildable").bind(Util.parseBoolean).orSome(false),
            child(root, "lastBuild").bind(BuildXml.buildXml),
            child(root, "lastCompletedBuild").bind(BuildXml.buildXml),
            child(root, "lastFailedBuild").bind(BuildXml.buildXml),
            child(root, "lastSuccessfulBuild").bind(BuildXml.buildXml),
            child(root, "lastUnsuccessfulBuild").bind(BuildXml.buildXml));
    }
}


package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.jenkins.xml.*;
import io.trygvis.esper.testing.jenkins.xml.JenkinsJobXml.*;
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

import static fj.P.*;
import static fj.data.Option.*;
import static io.trygvis.esper.testing.util.HttpClient.*;
import static java.lang.System.*;
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
                Feed feed = (Feed) parser.parse(inputStream).getRoot().complete();
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

    public Option<P2<List<JenkinsEntryXml>, byte[]>> fetchRss(URI url) {
        return jenkinsEntryXmlClient.fetch(url);
    }

    public Option<P2<JenkinsXml, byte[]>> fetchJobs(URI url) {
        Option<P2<Document, byte[]>> d = xmlHttpClient.fetch(url);

        if (d.isNone()) {
            return none();
        }

        Element root = d.some()._1().getRootElement();

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

        return some(p(new JenkinsXml(
                Option.fromNull(root.getChildText("nodeName")),
                Option.fromNull(root.getChildText("nodeDescription")),
                Option.fromNull(root.getChildText("description")), jobs), d.some()._2()));
    }

    public Option<P2<JenkinsJobXml, byte[]>> fetchJob(URI url) {
        Option<P2<Document, byte[]>> d = xmlHttpClient.fetch(url);

        if (d.isNone()) {
            return Option.none();
        }

        Element root = d.some()._1().getRootElement();

        String name = root.getName();

        switch (name) {
            case "freeStyleProject":
                return some(p(JenkinsJobXml.parse(url, JenkinsJobType.FREE_STYLE, root), d.some()._2()));
            case "mavenModuleSet":
                return some(p(JenkinsJobXml.parse(url, JenkinsJobType.MAVEN_MODULE_SET, root), d.some()._2()));
            case "mavenModule":
                return some(p(JenkinsJobXml.parse(url, JenkinsJobType.MAVEN_MODULE, root), d.some()._2()));
            case "matrixProject":
                return some(p(JenkinsJobXml.parse(url, JenkinsJobType.MATRIX, root), d.some()._2()));
            case "matrixConfiguration":
                return some(p(JenkinsJobXml.parse(url, JenkinsJobType.MATRIX_CONFIGURATION, root), d.some()._2()));
            default:
                logger.warn("Unknown project type: " + name);
                return Option.none();
        }
    }

    public Option<P2<JenkinsBuildXml, byte[]>> fetchBuild(URI url) {
        final Option<P2<Document, byte[]>> d = xmlHttpClient.fetch(url);

        if (d.isNone()) {
            return Option.none();
        }

        Element root = d.some()._1().getRootElement();

        String name = root.getName();

        switch (name) {
            // I don't know the different between a "matrix build" and "matrix run"
            case "matrixBuild":
            case "matrixRun":
            case "mavenModuleSetBuild":
            case "mavenBuild":
            case "freeStyleBuild":
                return JenkinsBuildXml.parse(root.getDocument()).map(new F<JenkinsBuildXml, P2<JenkinsBuildXml, byte[]>>() {
                    public P2<JenkinsBuildXml, byte[]> f(JenkinsBuildXml x) {
                        return p(x, d.some()._2());
                    }
                });
            default:
                logger.warn("Unknown build type: " + name);
                return Option.none();
        }
    }
}

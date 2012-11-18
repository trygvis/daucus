package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import static io.trygvis.esper.testing.XmlUtil.*;
import static java.lang.Integer.*;
import static org.apache.commons.lang.StringUtils.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.h2.util.*;
import org.jdom2.*;
import org.jdom2.input.*;

import javax.xml.stream.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class JenkinsClient {
    private static final XMLInputFactory xmlReader = XMLInputFactory.newFactory();
    private static final StAXStreamBuilder streamBuilder = new StAXStreamBuilder();
    private final HTTPCache http;

    private boolean debugXml;

    public JenkinsClient(HTTPCache http) {
        this.http = http;
        this.debugXml = false;
    }

    public void setDebugXml(boolean debugXml) {
        this.debugXml = debugXml;
    }

    public JenkinsXml fetchJobs(URI uri) throws XMLStreamException, JDOMException, IOException {
        Element root = fetchXml(uri).getRootElement();

        List<JenkinsJobEntryXml> jobs = new ArrayList<>();
        for (Element job : root.getChildren("job")) {
            String name = trimToNull(job.getChildText("name"));
            String url = trimToNull(job.getChildText("url"));
            String color = trimToNull(job.getChildText("color"));

            if (name == null || url == null || color == null) {
                continue;
            }

            jobs.add(new JenkinsJobEntryXml(name, url, color));
        }

        return new JenkinsXml(
            Option.fromNull(root.getChildText("nodeName")),
            Option.fromNull(root.getChildText("nodeDescription")),
            Option.fromNull(root.getChildText("description")), jobs);
    }

    public JenkinsJobXml fetchJob(URI uri) throws IOException, JDOMException, XMLStreamException {
        Element root = fetchXml(uri).getRootElement();

        switch (root.getName()) {
            case "freeStyleProject":
                return FreeStyleProjectXml.parse(root);
            case "mavenModuleSet":
                return MavenModuleSetXml.parse(root);
            default:
                throw new IOException("Unknown project type: " + root.getName());
        }
    }

    private Document fetchXml(URI uri) throws IOException, XMLStreamException, JDOMException {
        HTTPResponse response = null;

        try {
            response = http.execute(new HTTPRequest(uri));

            if (response.getStatus().getCode() != 200) {
                throw new IOException("Did not get 200 back, got " + response.getStatus().getCode());
            }

            InputStream stream = response.getPayload().getInputStream();

            if (debugXml) {
                int size;
                try {
                    size = parseInt(response.getHeaders().getFirstHeader("Content-Length").getValue());
                } catch (Throwable e) {
                    size = 10 * 1024;
                }

                // TODO: Pretty print

                ByteArrayOutputStream buffer = new ByteArrayOutputStream(size);
                IOUtils.copy(stream, buffer);
                byte[] bytes = buffer.toByteArray();
                System.out.println("------------------------------------------------");
                System.out.write(bytes);
                System.out.println();
                System.out.println("------------------------------------------------");
                stream = new ByteArrayInputStream(bytes);
            }

            return streamBuilder.build(xmlReader.createXMLStreamReader(stream));
        } catch (HTTPException e) {
            throw new IOException(e);
        } finally {
            if (response != null) {
                response.consume();
            }
        }
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

abstract class JenkinsJobXml {
    public final Option<String> description;
    public final Option<String> displayName;
    public final Option<String> name;
    public final Option<String> url;
    public final Option<String> color;
    public final Option<BuildXml> lastBuild;
    public final Option<BuildXml> lastCompletedBuild;
    public final Option<BuildXml> lastFailedBuild;
    public final Option<BuildXml> lastSuccessfulBuild;
    public final Option<BuildXml> lastUnsuccessfulBuild;

    protected JenkinsJobXml(Option<String> description, Option<String> displayName, Option<String> name, Option<String> url, Option<String> color, Option<BuildXml> lastBuild, Option<BuildXml> lastCompletedBuild, Option<BuildXml> lastFailedBuild, Option<BuildXml> lastSuccessfulBuild, Option<BuildXml> lastUnsuccessfulBuild) {
        this.description = description;
        this.displayName = displayName;
        this.name = name;
        this.url = url;
        this.color = color;
        this.lastBuild = lastBuild;
        this.lastCompletedBuild = lastCompletedBuild;
        this.lastFailedBuild = lastFailedBuild;
        this.lastSuccessfulBuild = lastSuccessfulBuild;
        this.lastUnsuccessfulBuild = lastUnsuccessfulBuild;
    }
}

class BuildXml {
    public final int number;
    public final URI url;
    public static F<Element, Option<BuildXml>> buildXml = new F<Element, Option<BuildXml>>() {
        public Option<BuildXml> f(Element element) {
            Option<Integer> number = childText(element, "number").bind(XmlUtil.parseInt);
            Option<URI> url = childText(element, "url").bind(parseUri);

            if(number.isNone() || url.isNone()) {
                return Option.none();
            }

            return Option.some(new BuildXml(number.some(), url.some()));
        }
    };

    BuildXml(int number, URI url) {
        this.number = number;
        this.url = url;
    }
}

class FreeStyleProjectXml extends JenkinsJobXml {
    FreeStyleProjectXml(Option<String> description, Option<String> displayName, Option<String> name, Option<String> url, Option<String> color, Option<BuildXml> lastBuild, Option<BuildXml> lastCompletedBuild, Option<BuildXml> lastFailedBuild, Option<BuildXml> lastSuccessfulBuild, Option<BuildXml> lastUnsuccessfulBuild) {
        super(description, displayName, name, url, color, lastBuild, lastCompletedBuild, lastFailedBuild, lastSuccessfulBuild, lastUnsuccessfulBuild);
    }

    public static JenkinsJobXml parse(Element root) {
        return new FreeStyleProjectXml(
            childText(root, "description"),
            childText(root, "displayName"),
            childText(root, "name"),
            childText(root, "url"),
            childText(root, "color"),
            child(root, "lastBuild").bind(BuildXml.buildXml),
            child(root, "lastCompletedBuild").bind(BuildXml.buildXml),
            child(root, "lastFailedBuild").bind(BuildXml.buildXml),
            child(root, "lastSuccessfulBuild").bind(BuildXml.buildXml),
            child(root, "lastUnsuccessfulBuild").bind(BuildXml.buildXml));
    }
}

class MavenModuleSetXml extends JenkinsJobXml {
    MavenModuleSetXml(Option<String> description, Option<String> displayName, Option<String> name, Option<String> url, Option<String> color, Option<BuildXml> lastBuild, Option<BuildXml> lastCompletedBuild, Option<BuildXml> lastFailedBuild, Option<BuildXml> lastSuccessfulBuild, Option<BuildXml> lastUnsuccessfulBuild) {
        super(description, displayName, name, url, color, lastBuild, lastCompletedBuild, lastFailedBuild, lastSuccessfulBuild, lastUnsuccessfulBuild);
    }

    public static JenkinsJobXml parse(Element root) {
        return new MavenModuleSetXml(
            childText(root, "description"),
            childText(root, "displayName"),
            childText(root, "name"),
            childText(root, "url"),
            childText(root, "color"),
            child(root, "lastBuild").bind(BuildXml.buildXml),
            child(root, "lastCompletedBuild").bind(BuildXml.buildXml),
            child(root, "lastFailedBuild").bind(BuildXml.buildXml),
            child(root, "lastSuccessfulBuild").bind(BuildXml.buildXml),
            child(root, "lastUnsuccessfulBuild").bind(BuildXml.buildXml));
    }
}

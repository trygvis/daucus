package io.trygvis.esper.testing.jenkins;

import static org.apache.commons.lang.StringUtils.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.util.*;
import org.jdom2.*;
import org.jdom2.input.*;

import javax.xml.stream.*;
import java.net.*;
import java.util.*;

public class JenkinsClient {
    private static final XMLInputFactory xmlReader = XMLInputFactory.newFactory();
    private static final StAXStreamBuilder streamBuilder = new StAXStreamBuilder();
    private final HTTPCache http;
    private final URI apiXmlUri;

    public JenkinsClient(HTTPCache http, URI jenkinsUri) {
        this.http = http;
        this.apiXmlUri = URIBuilder.fromURI(jenkinsUri).addRawPath("api/xml").toURI();
    }

    public List<JenkinsJobXml> fetchJobs() throws XMLStreamException, JDOMException {
        HTTPResponse response = http.execute(new HTTPRequest(apiXmlUri));

        if (response.getStatus().getCode() != 200) {
            throw new RuntimeException("Did not get 200 back, got " + response.getStatus().getCode());
        }

        Element doc = streamBuilder.build(xmlReader.createXMLStreamReader(response.getPayload().getInputStream())).getRootElement();

        List<JenkinsJobXml> jobs = new ArrayList<>();
        for (Element job : doc.getChildren("job")) {
            String name = trimToNull(job.getChildText("name"));
            String url = trimToNull(job.getChildText("url"));
            String color = trimToNull(job.getChildText("color"));

            if (name == null || url == null || color == null) {
                continue;
            }

            jobs.add(new JenkinsJobXml(name, url, color));
        }

        return jobs;
    }
}

class JenkinsJobXml {
    public final String name;
    public final String url;
    public final String color;

    JenkinsJobXml(String name, String url, String color) {
        this.name = name;
        this.url = url;
        this.color = color;
    }
}
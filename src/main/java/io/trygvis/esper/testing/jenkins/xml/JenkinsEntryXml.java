package io.trygvis.esper.testing.jenkins.xml;

import java.net.URI;

import org.joda.time.DateTime;

public class JenkinsEntryXml {
    public final String id;
    public final DateTime timestamp;
    public final URI url;

    public JenkinsEntryXml(String id, DateTime timestamp, URI url) {
        this.id = id;
        this.timestamp = timestamp;
        this.url = url;
    }
}

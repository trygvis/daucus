package io.trygvis.esper.testing.nexus;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.*;
import static io.trygvis.esper.testing.nexus.ArtifactXml.repositoryFilter;
import io.trygvis.esper.testing.util.*;
import junit.framework.*;
import org.jdom2.*;
import org.joda.time.*;

import java.io.*;
import java.util.*;

public class TestXmlParsing extends TestCase {
    public void testProjectParsing() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/nexus/search-1.xml")) {
            ArtifactSearchResult result = SearchNGResponseParser.parseDocument(stream);

            List<ArtifactXml> list = result.artifacts;

            assertNotNull(list);
            assertEquals(132, list.size());

            ArtifactXml artifact = list.get(0);
            assertEquals("org.codehaus.mojo.hibernate3", artifact.id.groupId);
            assertEquals("maven-hibernate3-jdk15", artifact.id.artifactId);
            assertEquals("2.0-alpha-1", artifact.id.version);

            artifact = list.get(4);
            assertEquals("org.codehaus.mojo.hibernate3", artifact.id.groupId);
            assertEquals("maven-hibernate3", artifact.id.artifactId);
            assertEquals("2.0-alpha-1", artifact.id.version);
            assertEquals(2, artifact.hits.size());
            assertEquals("appfuse-releases", artifact.hits.get(0).repositoryId);
            assertEquals(1, artifact.hits.get(0).files.size());
            assertTrue(artifact.hits.get(0).files.get(0).classifier.isNone());
            assertEquals("pom", artifact.hits.get(0).files.get(0).extension);

            assertEquals(2, artifact.hits.size());
            ArrayList<ArtifactXml> filtered = newArrayList(filter(list, repositoryFilter("appfuse-releases")));
            assertEquals(5, filtered.size());

            FlatArtifact flatArtifact = filtered.get(0).flatten("appfuse-releases").some();
            assertEquals("org.codehaus.mojo.hibernate3", flatArtifact.id.groupId);
            assertEquals("maven-hibernate3-jdk15", flatArtifact.id.artifactId);
            assertEquals("2.0-alpha-1", flatArtifact.id.version);
            assertEquals(2, flatArtifact.files.size());
        }
    }

    public void testTimelineParsing() throws Exception {
        XmlParser parser = new XmlParser();

        try (InputStream stream = getClass().getResourceAsStream("/nexus/recentlyDeployedArtifacts.xml")) {
            Document document = parser.parseDocument(stream).some();

            List<Element> items = document.getRootElement().getChild("channel").getChildren("item");

            NewSnapshotEvent e = (NewSnapshotEvent) NexusFeedParser.parseEvent(items.get(0)).some();
            NexusEvent event = e.event;
            assertEquals("2012-12-04T12:26:40.000Z", event.date.withZone(DateTimeZone.UTC).toString());
            assertEquals("developer", event.creator);
            assertEquals("org.example", event.artifactId.groupId);
            assertEquals("example", event.artifactId.artifactId);
            assertEquals("1.0-SNAPSHOT", event.artifactId.version);
            assertEquals("2012-12-04 12:26:40", e.snapshotTimestamp.toString("yyyy-MM-dd hh:mm:ss"));
            assertEquals(536, e.buildNumber);

            e = (NewSnapshotEvent) NexusFeedParser.parseEvent(items.get(1)).some();
            event = e.event;
            assertEquals("org.example", event.artifactId.groupId);
            assertEquals("example", event.artifactId.artifactId);
            assertEquals("1.0-SNAPSHOT", event.artifactId.version);
            assertEquals("2012-12-04 12:26:40", e.snapshotTimestamp.toString("yyyy-MM-dd hh:mm:ss"));
            assertEquals(536, e.buildNumber);

            NewReleaseEvent nre = (NewReleaseEvent) NexusFeedParser.parseEvent(items.get(2)).some();
            event = nre.event;
            assertEquals("org.example", event.artifactId.groupId);
            assertEquals("example", event.artifactId.artifactId);
            assertEquals("1.10", event.artifactId.version);

            assertEquals(3, items.size());
        }
    }
}

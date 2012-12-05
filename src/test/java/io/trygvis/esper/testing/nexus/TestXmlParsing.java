package io.trygvis.esper.testing.nexus;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.*;
import static io.trygvis.esper.testing.nexus.ArtifactXml.repositoryFilter;
import io.trygvis.esper.testing.util.*;
import junit.framework.*;
import org.jdom2.*;

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

            NexusEvent event = NexusFeedParser.parseEvent(document.getRootElement().getChild("channel").getChild("item")).some();

            assertTrue(event instanceof NewSnapshotEvent);
            NewSnapshotEvent nse = (NewSnapshotEvent) event;
            assertEquals("org.example", nse.artifactId.groupId);
            assertEquals("example", nse.artifactId.artifactId);
            assertEquals("1.0-SNAPSHOT", nse.artifactId.version);
            assertEquals("20121204.122640-536", nse.snapshotTimestamp);
        }
    }
}

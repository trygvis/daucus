package io.trygvis.esper.testing.gitorious;

import fj.data.*;
import junit.framework.*;
import org.apache.abdera.*;
import org.dom4j.*;

import java.io.*;
import java.util.*;
import java.util.List;

public class TestXmlParsing extends TestCase {
    public void testProjectParsing() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/gitorious/projects-2.xml")) {
            Document document = GitoriousClient.xmlReader.readDocument(stream);

            List<GitoriousProjectXml> projects = GitoriousProjectXml.projectsFromXml(document.getRootElement());

            assertNotNull(projects);
            assertEquals(20, projects.size());

            GitoriousProjectXml project = projects.get(3);
            assertEquals("aed-ii", project.slug);
            assertEquals(2, project.repositories.size());
        }
    }

    public void testEventParsing() throws Exception {
        GitoriousAtomFeedParser parser = new GitoriousAtomFeedParser(new Abdera());
        try (InputStream stream = getClass().getResourceAsStream("/gitorious/esper-test-project.atom")) {

            List<GitoriousEvent> events = parser.parseStream(stream, Option.<Date>none(), "esper-test-project", "esper-test-project");

            assertEquals(5, events.size());

            GitoriousPush p0 = (GitoriousPush) events.get(0);
            assertEquals("tag:gitorious.org,2005:Event/43390557", p0.entryId);
            assertEquals("trygvis", p0.who);
            assertEquals("dd6f41a45587f3f4d81ba7c0a874fcaf94e67365", p0.from);
            assertEquals("0d3de9c126c6f84e46e3f92244b4d99a4a3a3aa5", p0.to);
            assertEquals("my-branch", p0.branch);

            GitoriousPush p3 = (GitoriousPush) events.get(3);
            assertEquals("tag:gitorious.org,2005:Event/43390409", p3.entryId);
            assertEquals("trygvis", p3.who);
            assertEquals("7054468bc18ae6e66aeccecc87896a90b21f2101", p3.from);
            assertEquals("4aa8a70c00a9527035e3f9b2fb69bbc4779aa090", p3.to);
            assertEquals("master", p3.branch);
        }
    }
}

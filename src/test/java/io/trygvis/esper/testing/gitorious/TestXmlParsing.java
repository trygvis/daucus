package io.trygvis.esper.testing.gitorious;

import junit.framework.*;
import org.dom4j.*;

import java.io.*;
import java.util.*;

public class TestXmlParsing extends TestCase {
    public void testProjectParsing() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/gitorious/projects-2.xml")) {
            Document document = GitoriousClient.xmlReader.readDocument(stream);

            List<GitoriousProject> projects = GitoriousProject.projectsFromXml("http://gitorious.org", document.getRootElement());

            assertNotNull(projects);
            assertEquals(20, projects.size());

            GitoriousProject project = projects.get(3);
            assertEquals("aed-ii", project.slug);
            assertEquals(2, project.repositories.size());
        }
    }
}

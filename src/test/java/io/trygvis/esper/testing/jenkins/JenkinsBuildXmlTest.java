package io.trygvis.esper.testing.jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.jdom2.Document;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.chrono.ISOChronology;

import fj.data.Option;
import io.trygvis.esper.testing.util.XmlParser;
import junit.framework.TestCase;

import static org.joda.time.DateTimeZone.forOffsetHours;
import static org.joda.time.DateTimeZone.forOffsetHoursMinutes;
import static org.joda.time.chrono.ISOChronology.getInstance;

public class JenkinsBuildXmlTest extends TestCase {
    XmlParser parser = new XmlParser();
    ISOChronology minus6 = getInstance(forOffsetHours(-6));
    ISOChronology minus5 = getInstance(forOffsetHours(-5));
    ISOChronology plus530 = getInstance(forOffsetHoursMinutes(5, 30));

    public Option<Document> f(InputStream inputStream) {
        return parser.parseDocument(inputStream);
    }

    public void testYo() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/jenkins/build/build-with-git-commits.xml")) {
            Option<JenkinsBuildXml> option = JenkinsBuildXml.parse(parser.parseDocument(is).some().getRootElement());

            assertTrue(option.isSome());
            JenkinsBuildXml build = option.some();

            assertEquals(URI.create("http://ci.jruby.org/job/jruby-dist-master/1085/"), build.url);
            assertEquals(1085, build.number);
            assertTrue(build.result.isSome());
            assertEquals(488824, build.duration);
            assertTrue(build.changeSet.isSome());
            JenkinsBuildXml.ChangeSetXml changeSet = build.changeSet.some();
            assertTrue(changeSet.revision.isNone());
            assertEquals(8, changeSet.items.size());

            assertItem(changeSet.items.get(0), "df6d3e8773dff6cc727a6ca4d8eaa8b5f970138d", "Add missing jnr-enxio to NB project.", new DateTime(2012, 12, 5, 10, 40, 58, 0, minus6));
            assertItem(changeSet.items.get(1), "54f6ce8c6e9a2c068c6b1bdfa096dcabb4e0ef1c", "Added description for vertexFor method", new DateTime(2012, 12, 5, 10, 56, 3, 0, plus530));
            assertItem(changeSet.items.get(2), "b6edf4a29157ef5ecd4e050c79f9353200ed0daf", "Added spec for allData method of DirectedGraph.java", new DateTime(2012, 12, 5, 10, 56, 3, 0, plus530));
            assertItem(changeSet.items.get(3), "9deef648a66d8fd4ed1b119419410b28492b87b4", "Added spec for removeEdge(Edge edge) method in DirectedGraph.java", new DateTime(2012, 12, 5, 11, 7, 34, 0, plus530));
            assertItem(changeSet.items.get(4), "9a3eb797ce136349f3866fc2ae9b35be360fb3df", "Added spec for getInorderData method of DirectedGraph.java", new DateTime(2012, 12, 5, 11, 20, 17, 0, plus530));
            assertItem(changeSet.items.get(5), "41b5de23dd2d7ccbc170252a43b8996316b93075", "No need to look up TZ here. In all cases leading up to here,", new DateTime(2012, 12, 6, 0, 5, 37, 0, minus5));
            assertItem(changeSet.items.get(6), "def4c054ae82848c92b015a3267ace2c2cedd193", "Identify the correct JIRA ticket.", new DateTime(2012, 12, 6, 0, 8, 8, 0, minus5));
            assertItem(changeSet.items.get(7), "82f12220d01c2c07398107fa5f5a2d50feb7c8c4", "As ugly as it might be, maintaining a map of exceptional time zone", new DateTime(2012, 12, 6, 0, 17, 26, 0, minus5));
        }
    }

    private void assertItem(JenkinsBuildXml.ChangeSetItemXml item, String commitId, String msg, DateTime date) {
        assertEquals(commitId, item.commitId);
        assertEquals(msg, item.msg);
        assertEquals(date.toDateTime(item.date.getZone()), item.date);
    }
}

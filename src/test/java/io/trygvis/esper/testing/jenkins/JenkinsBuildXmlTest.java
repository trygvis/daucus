package io.trygvis.esper.testing.jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.jdom2.Document;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import fj.data.Option;
import io.trygvis.esper.testing.jenkins.xml.JenkinsBuildXml;
import io.trygvis.esper.testing.util.XmlParser;
import junit.framework.TestCase;

import static fj.data.Option.some;
import static org.joda.time.DateTimeZone.forOffsetHours;
import static org.joda.time.DateTimeZone.forOffsetHoursMinutes;
import static org.joda.time.chrono.ISOChronology.getInstance;
import static org.joda.time.chrono.ISOChronology.getInstanceUTC;

public class JenkinsBuildXmlTest extends TestCase {
    XmlParser parser = new XmlParser();
    ISOChronology minus6 = getInstance(forOffsetHours(-6));
    ISOChronology minus5 = getInstance(forOffsetHours(-5));
    ISOChronology plus530 = getInstance(forOffsetHoursMinutes(5, 30));
    ISOChronology utc = getInstanceUTC();

    Option<JenkinsBuildXml.AuthorXml> con = some(new JenkinsBuildXml.AuthorXml("http://ci.jruby.org/user/Charles%20Oliver%20Nutter", "Charles Oliver Nutter"));
    Option<JenkinsBuildXml.AuthorXml> csonpatki = some(new JenkinsBuildXml.AuthorXml("http://ci.jruby.org/user/csonpatki", "csonpatki"));
    Option<JenkinsBuildXml.AuthorXml> hasari = some(new JenkinsBuildXml.AuthorXml("http://ci.jruby.org/user/hasari", "Hiro Asari"));
    Option<JenkinsBuildXml.AuthorXml> markrmiller = some(new JenkinsBuildXml.AuthorXml("https://builds.apache.org/user/markrmiller", "markrmiller"));

    public Option<Document> f(InputStream inputStream) {
        return parser.parseDocument(inputStream);
    }

    public void testGitCommitParsing() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/jenkins/build/build-with-git-commits.xml")) {
            Option<JenkinsBuildXml> option = parser.parseDocument(is).bind(JenkinsBuildXml.parse);

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

            assertItem(changeSet.items.get(0), "df6d3e8773dff6cc727a6ca4d8eaa8b5f970138d", "Add missing jnr-enxio to NB project.", new DateTime(2012, 12, 5, 10, 40, 58, 0, minus6), con);
            assertItem(changeSet.items.get(1), "54f6ce8c6e9a2c068c6b1bdfa096dcabb4e0ef1c", "Added description for vertexFor method", new DateTime(2012, 12, 5, 10, 56, 3, 0, plus530), csonpatki);
            assertItem(changeSet.items.get(2), "b6edf4a29157ef5ecd4e050c79f9353200ed0daf", "Added spec for allData method of DirectedGraph.java", new DateTime(2012, 12, 5, 10, 56, 3, 0, plus530), csonpatki);
            assertItem(changeSet.items.get(3), "9deef648a66d8fd4ed1b119419410b28492b87b4", "Added spec for removeEdge(Edge edge) method in DirectedGraph.java", new DateTime(2012, 12, 5, 11, 7, 34, 0, plus530), csonpatki);
            assertItem(changeSet.items.get(4), "9a3eb797ce136349f3866fc2ae9b35be360fb3df", "Added spec for getInorderData method of DirectedGraph.java", new DateTime(2012, 12, 5, 11, 20, 17, 0, plus530), csonpatki);
            assertItem(changeSet.items.get(5), "41b5de23dd2d7ccbc170252a43b8996316b93075", "No need to look up TZ here. In all cases leading up to here,", new DateTime(2012, 12, 6, 0, 5, 37, 0, minus5), hasari);
            assertItem(changeSet.items.get(6), "def4c054ae82848c92b015a3267ace2c2cedd193", "Identify the correct JIRA ticket.", new DateTime(2012, 12, 6, 0, 8, 8, 0, minus5), hasari);
            assertItem(changeSet.items.get(7), "82f12220d01c2c07398107fa5f5a2d50feb7c8c4", "As ugly as it might be, maintaining a map of exceptional time zone", new DateTime(2012, 12, 6, 0, 17, 26, 0, minus5), hasari);

            assertTrue(changeSet.items.get(0).author.isSome());
            assertEquals("http://ci.jruby.org/user/Charles%20Oliver%20Nutter", changeSet.items.get(0).author.some().absoluteUrl);
        }
    }

    public void testSvnCommitParsing() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/jenkins/build/build-with-subversion-commits.xml")) {
            Option<JenkinsBuildXml> option = parser.parseDocument(is).bind(JenkinsBuildXml.parse);

            assertTrue(option.isSome());
            JenkinsBuildXml build = option.some();

            assertEquals(URI.create("https://builds.apache.org/job/Lucene-Solr-Tests-4.x-Java6/1102/"), build.url);
            assertEquals(1102, build.number);
            assertTrue(build.result.isSome());
            assertEquals(1646526, build.duration);
            assertTrue(build.changeSet.isSome());
            JenkinsBuildXml.ChangeSetXml changeSet = build.changeSet.some();
            assertTrue(changeSet.revision.isSome());
            assertEquals("http://svn.apache.org/repos/asf/lucene/dev/branches/branch_4x", changeSet.revision.some().module);
            assertEquals(1419960, changeSet.revision.some().revision);
            assertEquals(3, changeSet.items.size());

            assertItem(changeSet.items.get(0), "1419960", "SOLR-2986: Add MoreLikeThis to warning about features that require uniqueKey. Also, change the warning to warn log level.", new DateTime(2012, 12, 11, 1, 8, 10, 682, utc), markrmiller);
            assertItem(changeSet.items.get(1), "1419953", "SOLR-4071: Validate that name is pass to Collections API create, and behave the same way as on startup when collection.configName is not explicitly passed.", new DateTime(2012, 12, 11, 0, 56, 19, 684, utc), markrmiller);
            assertItem(changeSet.items.get(2), "1419940", "SOLR-3948: Calculate/display deleted documents in admin interface.", new DateTime(2012, 12, 11, 0, 10, 12, 700, utc), markrmiller);
        }
    }

    private void assertItem(JenkinsBuildXml.ChangeSetItemXml item, String commitId, String msg, DateTime date, Option<JenkinsBuildXml.AuthorXml> author) {
        assertEquals(commitId, item.commitId);
        assertEquals(msg, item.msg);
        assertEquals(date.toDateTime(item.date.getZone()), item.date);

        assertEquals("author.isSome()", author.isSome(), item.author.isSome());
        if(item.author.isSome()) {
            assertEquals(author.some().absoluteUrl, item.author.some().absoluteUrl);
            assertEquals(author.some().fullName, item.author.some().fullName);
        }
    }
}

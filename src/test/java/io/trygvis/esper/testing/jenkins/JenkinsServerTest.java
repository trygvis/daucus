package io.trygvis.esper.testing.jenkins;

import junit.framework.*;

import static io.trygvis.esper.testing.jenkins.JenkinsServer.*;

public class JenkinsServerTest extends TestCase {
    public void testUrlGeneration() {
        assertEquals("https://builds.apache.org/job/james-server-trunk/", createJobUrl("https://builds.apache.org/job/james-server-trunk/org.apache.james$james-server-dnsservice-library/3417/").toASCIIString());

        assertEquals("http://ci.jruby.org/job/rails-master/", createJobUrl("http://ci.jruby.org/job/rails-master/component=activeresource,label=master/3577/").toASCIIString());

        assertEquals("http://ci.jruby.org/job/rails-master/", createJobUrl("http://ci.jruby.org/job/rails-master/3577/").toASCIIString());
    }
}

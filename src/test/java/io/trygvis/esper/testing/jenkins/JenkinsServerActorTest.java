package io.trygvis.esper.testing.jenkins;

import junit.framework.*;

import static io.trygvis.esper.testing.jenkins.JenkinsServerActor.*;

public class JenkinsServerActorTest extends TestCase {
    public void testUrlGeneration() {
//        assertEquals("https://jenkins.puppetlabs.com/job/Hiera%20%28master%29/74/api/xml", extrapolateJobUrlFromBuildUrl("https://jenkins.puppetlabs.com/job/Hiera%20%28master%29/facter=1.6.x,puppet=2.7.x,ruby=ruby-1.8.7,rvm=%23rvm/74/api/xml").toASCIIString());

        assertEquals("https://builds.apache.org/job/james-server-trunk/org.apache.james$james-server-dnsservice-library/", extrapolateJobUrlFromBuildUrl("https://builds.apache.org/job/james-server-trunk/org.apache.james$james-server-dnsservice-library/3417/").toASCIIString());

        assertEquals("http://ci.jruby.org/job/rails-master/component=activeresource,label=master/", extrapolateJobUrlFromBuildUrl("http://ci.jruby.org/job/rails-master/component=activeresource,label=master/3577/").toASCIIString());

        assertEquals("http://ci.jruby.org/job/rails-master/", extrapolateJobUrlFromBuildUrl("http://ci.jruby.org/job/rails-master/3577/").toASCIIString());
    }

    public void test2() {
        assertEquals("http://vm-pbuild-4:8080/job/admin-web/", extrapolateMavenModuleFromMavenModuleSetUrl("http://vm-pbuild-4:8080/job/admin-web/eu.nets.efaktura.adminweb$efaktura-admin-web-wsclient/"));
    }
}

package io.trygvis.esper.testing.jenkins;

import io.trygvis.esper.testing.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.client.*;

import java.net.*;
import java.util.*;

public class JenkinsImporter {
    private final JenkinsClient jenkinsClient;

    public static void main(String[] args) throws Exception {
        Main.configureLog4j();
        new JenkinsImporter().work();
    }

    public JenkinsImporter() {
        HTTPCache http = new HTTPCache(new MemoryCacheStorage(), HTTPClientResponseResolver.createMultithreadedInstance());
        jenkinsClient = new JenkinsClient(http, URI.create("https://builds.apache.org"));
    }

    private void work() throws Exception {
        List<JenkinsJobXml> jobs = jenkinsClient.fetchJobs();

        for (JenkinsJobXml job : jobs) {
            System.out.println("job.name = " + job.name);
        }
    }
}

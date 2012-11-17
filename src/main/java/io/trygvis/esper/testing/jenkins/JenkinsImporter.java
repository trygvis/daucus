package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.object.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.client.*;
import org.joda.time.*;

import java.net.*;
import java.net.URI;
import java.util.HashSet;
import java.util.concurrent.*;

public class JenkinsImporter {
    public static void main(String[] args) throws Exception {
        Main.configureLog4j();

//        HTTPClientResponseResolver resolver = HTTPClientResponseResolver.createMultithreadedInstance();
//        HTTPClientResponseResolver resolver = new HTTPClientResponseResolver(new HttpClient(new MultiThreadedHttpConnectionManager()));
        HTTPClientResponseResolver resolver = new HTTPClientResponseResolver(new HttpClient(new SimpleHttpConnectionManager()));
        HttpClientParams params = new HttpClientParams();
//        params.setConnectionManagerTimeout(1000);
        params.setSoTimeout(1000);
        resolver.getClient().setParams(params);
        HTTPCache http = new HTTPCache(new MemoryCacheStorage(), resolver);
        final JenkinsClient jenkinsClient = new JenkinsClient(http);

        jenkinsClient.setDebugXml(true);

        HashSet<URI> servers = new HashSet<>();
        servers.add(URI.create("https://builds.apache.org"));

        final ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(1);

        ObjectManager<URI, JenkinsServer> serverManager = new ObjectManager<>("JenkinsServer", servers, new ObjectFactory<URI, JenkinsServer>() {
            public JenkinsServer create(URI uri) {
                return new JenkinsServer(executorService, jenkinsClient, uri);
            }
        });
        final boolean[] shouldRun = new boolean[]{true};

        Runtime.getRuntime().addShutdownHook(new Thread() {
            {
                setName("Shutdown hoook");
            }

            public void run() {
                shouldRun[0] = false;
            }
        });

        while (shouldRun[0]) {
            for (JenkinsServer server : serverManager.getObjects()) {
                Option<P2<JenkinsXml, LocalDateTime>> o = server.getJenkins();

                if (o.isSome()) {
                    P2<JenkinsXml, LocalDateTime> p = o.some();
                    System.out.println("Last update: " + p._2() + ", jobs=" + p._1().jobs.size());
                } else {
                    System.out.println("Never updated: url=" + server.uri);
                }
            }

            Thread.sleep(1000);
        }

        serverManager.close();
        executorService.shutdownNow();
    }
}

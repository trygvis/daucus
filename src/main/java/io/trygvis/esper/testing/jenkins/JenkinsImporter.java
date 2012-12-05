package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.object.*;
import io.trygvis.esper.testing.util.*;
import static java.lang.Thread.currentThread;
import org.codehaus.httpcache4j.cache.*;
import org.joda.time.*;

import java.net.URI;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class JenkinsImporter {
    public static void main(String[] args) throws Exception {
        Config config = Config.loadFromDisk();

        HTTPCache httpCache = HttpClient.createHttpCache(config);

        final JenkinsClient jenkinsClient = new JenkinsClient(httpCache);

        HashSet<URI> servers = new HashSet<>();
        servers.add(URI.create("https://builds.apache.org"));

        final ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(5);

        ObjectManager<URI, JenkinsServer> serverManager = new ObjectManager<>("JenkinsServer", servers, new ObjectFactory<URI, JenkinsServer>() {
            public JenkinsServer create(URI uri) {
                return new JenkinsServer(executorService, jenkinsClient, uri);
            }
        });

        final AtomicBoolean shouldRun = new AtomicBoolean(true);
        config.addShutdownHook(currentThread(), shouldRun);

        while (shouldRun.get()) {
            for (JenkinsServer server : serverManager.getObjects()) {
                Option<P2<JenkinsXml, LocalDateTime>> o = server.getJenkins();

                if (o.isSome()) {
                    P2<JenkinsXml, LocalDateTime> p = o.some();
                    System.out.println("Last update: " + p._2() + ", jobs=" + p._1().jobs.size());
                } else {
                    System.out.println("Never updated: url=" + server.uri);
                }
            }

            synchronized (shouldRun) {
                shouldRun.wait(1000);
            }
        }

        serverManager.close();
        executorService.shutdownNow();
    }
}

package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import static io.trygvis.esper.testing.Http.http;
import io.trygvis.esper.testing.object.*;
import org.joda.time.*;

import java.net.URI;
import java.util.HashSet;
import java.util.concurrent.*;

public class JenkinsImporter {
    public static void main(String[] args) throws Exception {
        Main.configureLog4j();

        final JenkinsClient jenkinsClient = new JenkinsClient(http);

        jenkinsClient.setDebugXml(false);

        HashSet<URI> servers = new HashSet<>();
        servers.add(URI.create("https://builds.apache.org"));

        final ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(5);

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

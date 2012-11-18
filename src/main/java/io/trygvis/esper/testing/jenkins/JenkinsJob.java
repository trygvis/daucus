package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import static fj.data.Option.*;
import static java.lang.System.currentTimeMillis;
import org.codehaus.httpcache4j.util.*;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class JenkinsJob implements Closeable {

    private final Logger logger = LoggerFactory.getLogger("jenkins.job");
    private final JenkinsClient client;
    private final URI uri;

    private Option<JenkinsJobXml> latestStatus = none();
    private final ScheduledFuture<?> future;

    public JenkinsJob(ScheduledExecutorService executorService, JenkinsClient client, URI uri) {
        this.client = client;
        this.uri = URIBuilder.fromURI(uri).addRawPath("api/xml").toURI();

        long initialDelay = (long) Math.random() + 1;
        long period = (long) (Math.random() * 100d) + 1;
        future = executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                JenkinsJob.this.doWork();
            }
        }, initialDelay, period, TimeUnit.SECONDS);
    }

    public Option<JenkinsJobXml> getStatus() {
        return latestStatus;
    }

    public void close() throws IOException {
        future.cancel(true);
    }

    private void doWork() {

        String name = latestStatus.isSome() && latestStatus.some().name.isSome() ?
            latestStatus.some().name.some() : uri.toASCIIString();

        try {
            logger.info("Updating " + name);
            long start = currentTimeMillis();
            latestStatus = some(client.fetchJob(uri));
            long end = currentTimeMillis();
            logger.info("Updated " + name + " in " + (end - start) + "ms");
        } catch (Throwable e) {
            logger.warn("Error updating " + name, e);
        }
    }
}

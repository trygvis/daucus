package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import static fj.data.Option.*;
import static io.trygvis.esper.testing.jenkins.JenkinsClient.apiXml;
import static java.lang.System.currentTimeMillis;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class JenkinsJob implements Closeable {

    private final Logger logger = LoggerFactory.getLogger("jenkins.job");
    private final JenkinsClient client;
    private final URI url;

    private Option<JenkinsJobXml> latestStatus = none();
    private final ScheduledFuture<?> future;

    public JenkinsJob(ScheduledExecutorService executorService, JenkinsClient client, URI url) {
        this.client = client;
        this.url = apiXml(url);

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
            latestStatus.some().name.some() : url.toASCIIString();

        try {
            logger.info("Updating " + name);
            long start = currentTimeMillis();
            latestStatus = client.fetchJob(url);
            long end = currentTimeMillis();
            logger.info("Updated " + name + " in " + (end - start) + "ms");
        } catch (Throwable e) {
            logger.warn("Error updating " + name, e);
        }
    }
}

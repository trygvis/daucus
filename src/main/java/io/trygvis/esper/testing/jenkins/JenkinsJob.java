package io.trygvis.esper.testing.jenkins;

import org.codehaus.httpcache4j.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class JenkinsJob implements Closeable {

    private final JenkinsClient client;
    private final URI uri;

    private JenkinsJobXml latestStatus;
    // private boolean shouldRun = true;
    // private final Thread thread;
    private final ScheduledFuture<?> future;

    public JenkinsJob(ScheduledExecutorService executorService, JenkinsClient client, URI uri) {
        this.client = client;
        this.uri = URIBuilder.fromURI(uri).addRawPath("api/xml").toURI();

        long initialDelay = (long) Math.random() + 1;
        long period = (long) (Math.random() * 10d) + 1;
        future = executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                JenkinsJob.this.doWork();
            }
        }, initialDelay, period, TimeUnit.SECONDS);

//        thread = new Thread(new Runnable() {
//            public void run() {
//                JenkinsJob.this.run();
//            }
//        });
//        thread.setDaemon(true);
//        thread.start();
    }

    public JenkinsJobXml getLatestStatus() {
        return latestStatus;
    }

    /*
    public void close() throws IOException {
        shouldRun = false;
        thread.interrupt();
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                continue;
            }
        }
    }

    private void run() {
        Random r = new Random();
        while (shouldRun) {
            doWork();

            try {
                Thread.sleep(1000 + r.nextInt(10) * 1000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
    */

    public void close() throws IOException {
        future.cancel(true);
    }

    private void doWork() {
        try {
            latestStatus = client.fetchJob(uri);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}

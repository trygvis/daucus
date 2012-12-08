package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import static fj.data.Option.*;
import static io.trygvis.esper.testing.jenkins.JenkinsClient.apiXml;

import io.trygvis.esper.testing.object.*;
import org.joda.time.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;

public class JenkinsServerOld implements Closeable {

    private final JenkinsClient client;
    public final URI url;
    private final ObjectManager<URI, JenkinsJob> jobManager;

    private boolean shouldRun = true;
    private final Thread thread;

    private Option<P2<JenkinsXml, LocalDateTime>> jenkins = none();

    public JenkinsServerOld(final ScheduledExecutorService executorService, final JenkinsClient client, URI url) {
        this.client = client;
        this.url = apiXml(url);

        jobManager = new ObjectManager<>("JenkinsJob", Collections.<URI>emptySet(), new ObjectFactory<URI, JenkinsJob>() {
            public JenkinsJob create(URI url) {
                return new JenkinsJob(executorService, client, url);
            }
        });

        thread = new Thread(new Runnable() {
            public void run() {
                JenkinsServerOld.this.run();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

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
        while (shouldRun) {
            try {
                doWork();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }

            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public Option<P2<JenkinsXml, LocalDateTime>> getJenkins() {
        return jenkins;
    }

    public Collection<JenkinsJob> getJobs() {
        return jobManager.getObjects();
    }

    private void doWork() {
        try {
            JenkinsXml xml = client.fetchJobs(url);

            List<URI> jobUris = new ArrayList<>(xml.jobs.size());
            for (JenkinsJobEntryXml job : xml.jobs) {
                jobUris.add(URI.create(job.url));
            }

            this.jenkins = some(P.p(xml, new LocalDateTime()));

            jobManager.update(new HashSet<>(jobUris));
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        }
    }
}

package io.trygvis.esper.testing.jenkins;

import fj.*;
import fj.data.*;
import static fj.data.Option.*;
import io.trygvis.esper.testing.object.*;
import org.codehaus.httpcache4j.util.*;
import org.joda.time.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;

public class JenkinsServer implements Closeable {

    private final JenkinsClient client;
    public final URI uri;
    private final ObjectManager<URI, JenkinsJob> jobManager;

    private boolean shouldRun = true;
    private final Thread thread;

    private Option<P2<JenkinsXml, LocalDateTime>> jenkins = none();

    public JenkinsServer(final ScheduledExecutorService executorService, final JenkinsClient client, URI uri) {
        this.client = client;
        this.uri = URIBuilder.fromURI(uri).addRawPath("api/xml").toURI();

        jobManager = new ObjectManager<>("JenkinsJob", Collections.<URI>emptySet(), new ObjectFactory<URI, JenkinsJob>() {
            public JenkinsJob create(URI uri) {
                return new JenkinsJob(executorService, client, uri);
            }
        });

        thread = new Thread(new Runnable() {
            public void run() {
                JenkinsServer.this.run();
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
            JenkinsXml xml = client.fetchJobs(uri);

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

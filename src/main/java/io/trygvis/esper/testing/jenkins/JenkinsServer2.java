package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import io.trygvis.esper.testing.object.*;

import java.net.*;
import java.sql.*;
import java.util.List;

public class JenkinsServer2 implements TransactionalActor {
    private final JenkinsClient client;
    public final URI uri;

    public JenkinsServer2(JenkinsClient client, URI uri) {
        this.client = client;
        this.uri = uri;
    }

    public void act(Connection c) throws Exception {
        System.out.println("polling " + uri);

        Option<List<JenkinsEntryXml>> option = client.fetchRss(URI.create(uri.toASCIIString() + "/rssAll"));

        if(option.isNone()) {
            return;
        }

        List<JenkinsEntryXml> list = option.some();

        System.out.println("Got " + list.size() + " entries.");

        for (JenkinsEntryXml entry : list) {
            System.out.println("entry.uri = " + entry.uri);
        }
    }
}

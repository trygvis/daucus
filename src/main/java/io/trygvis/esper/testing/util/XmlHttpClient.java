package io.trygvis.esper.testing.util;

import fj.*;
import fj.data.*;
import org.codehaus.httpcache4j.cache.*;
import org.jdom2.*;

import java.io.*;
import java.net.*;

public class XmlHttpClient {

    private final HttpClient<Document> httpClient;

    public XmlHttpClient(HTTPCache http) {
        final XmlParser parser = new XmlParser();
        httpClient = new HttpClient<>(http, new F<InputStream, Option<Document>>() {
            public Option<Document> f(InputStream inputStream) {
                return parser.parseDocument(inputStream);
            }
        });
    }

    public Option<Document> fetch(URI uri) throws IOException {
        return httpClient.fetch(uri);
    }
}

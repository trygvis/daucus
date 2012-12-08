package io.trygvis.esper.testing.util;

import fj.*;
import fj.data.*;
import org.codehaus.httpcache4j.cache.*;
import org.jdom2.*;

import java.io.*;

public class XmlHttpClient extends HttpClient<Document> {

    public XmlHttpClient(HTTPCache http) {
        super(http, HttpClient.inputStreamOnly(new F<InputStream, Option<Document>>() {
            final XmlParser parser = new XmlParser();

            public Option<Document> f(InputStream inputStream) {
                return parser.parseDocument(inputStream);
            }
        }));
    }
}

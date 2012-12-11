package io.trygvis.esper.testing.util;

import fj.*;
import fj.data.*;
import org.codehaus.httpcache4j.cache.*;
import org.jdom2.*;
import org.slf4j.*;

import java.io.*;

import static java.lang.System.currentTimeMillis;

public class XmlHttpClient extends HttpClient<Document> {

    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    public XmlHttpClient(HTTPCache http) {
        super(http, HttpClient.inputStreamOnly(new F<InputStream, Option<Document>>() {
            final XmlParser parser = new XmlParser();

            public Option<Document> f(InputStream inputStream) {
                long start = currentTimeMillis();
                Option<Document> documents = parser.parseDocument(inputStream);
                long end = currentTimeMillis();
                logger.info("Parsed document in " + (end - start) + "ms.");
                return documents;
            }
        }));
    }
}

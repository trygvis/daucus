package io.trygvis.esper.testing.nexus;

import fj.data.*;
import static fj.data.Option.*;
import static io.trygvis.esper.testing.nexus.SearchNGResponseParser.*;
import io.trygvis.esper.testing.util.*;
import org.apache.commons.io.*;
import org.apache.commons.lang.*;
import static org.codehaus.httpcache4j.HTTPMethod.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.codehaus.httpcache4j.util.*;
import org.jdom2.*;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.stream.*;

public class NexusClient {
    private final HTTPCache http;

    private final URI nexusUrl;

    private final XmlHttpClient xmlHttpClient;

    public NexusClient(HTTPCache http, URI nexusUrl) {
        this.http = http;
        this.nexusUrl = nexusUrl;
        this.xmlHttpClient = new XmlHttpClient(http);
    }

    public NexusFeed fetchTimeline(String timeline, int count, int from) throws IOException {

        String args = "";

        if (count != 0) {
            args += (args.length() == 0 ? "?" : "&") + "count=" + count;
        }

        if (from != 0) {
            args += (args.length() == 0 ? "?" : "&") + "from=" + from;
        }

        System.out.println("args = " + args);
        URI uri = URI.create(nexusUrl.toASCIIString() + "/service/local/feeds/" + timeline + args);

        Option<Document> d = xmlHttpClient.fetch(uri);

        if (d.isNone()) {
            return new NexusFeed(Collections.<HasNexusEvent>emptyList());
        }

        Document document = d.some();

        return NexusFeedParser.parseDocument(document);
    }

    public ArtifactSearchResult fetchIndex(String groupId, Option<String> repositoryId) throws IOException {
        ArtifactSearchResult aggregate = fetchIndexPage(groupId, repositoryId, Option.<Integer>none());
        ArtifactSearchResult result = aggregate;

        while (result.artifacts.size() > 0) {
            result = fetchIndexPage(groupId, repositoryId, some(aggregate.artifacts.size()));
            aggregate = aggregate.append(result);
        }

        return aggregate;
    }

    public ArtifactSearchResult fetchIndexPage(String groupId, Option<String> repositoryId, Option<Integer> from) throws IOException {
        URIBuilder uriBuilder = URIBuilder.fromURI(nexusUrl).
                addRawPath("/service/local/lucene/search").
                addParameter("g", groupId);

        if (repositoryId.isSome()) {
            uriBuilder = uriBuilder.addParameter("repositoryId", repositoryId.some());
        }

        if (from.isSome()) {
            uriBuilder = uriBuilder.addParameter("from", from.some().toString());
        }

        HTTPResponse response = http.execute(new HTTPRequest(uriBuilder.toURI(), GET));

        int statusCode = response.getStatus().getCode();
        if (statusCode != 200) {
            throw new IOException("Got " + statusCode + " from Nexus search, expected 200.");
        }

        MIMEType mimeType = MIMEType.valueOf(StringUtils.trimToEmpty(response.getHeaders().getFirstHeaderValue("Content-Type")));
        if (!mimeType.getPrimaryType().equals("application") || !mimeType.getSubType().equals("xml")) {
            throw new IOException("Unexpected mime type: " + mimeType);
        }

        byte[] bytes = IOUtils.toByteArray(response.getPayload().getInputStream());

        try {
            ArtifactSearchResult result = parseDocument(new ByteArrayInputStream(bytes));
            System.out.println("Parsed out " + result.artifacts.size() + " artifacts.");
            return result;
        } catch (XMLStreamException e) {
            System.out.println("Unable to parse XML.");
            System.out.println(new String(bytes));
            throw new RuntimeException("Unable to parse XML.", e);
        }
    }
}

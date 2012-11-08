package io.trygvis.esper.testing.gitorious;

import static java.lang.System.*;
import org.apache.commons.io.*;
import static org.codehaus.httpcache4j.HTTPMethod.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.dom4j.*;
import org.dom4j.io.*;

import javax.xml.stream.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class GitoriousClient {
    public static final STAXEventReader xmlReader = new STAXEventReader();
    private final HTTPCache httpCache;
    private final String gitoriousUrl;
    private final String projectsUri;

    public GitoriousClient(HTTPCache httpCache, String gitoriousUrl) throws URISyntaxException {
        this.httpCache = httpCache;
        this.gitoriousUrl = new URI(gitoriousUrl).toASCIIString();
        this.projectsUri = gitoriousUrl + "/projects.xml";
    }

    public Set<GitoriousProject> findProjects() throws Exception {
        System.out.println("Fetching all projects");
        int page = 1;

        Set<GitoriousProject> all = new HashSet<>();
        while (page <= 10) {
            System.out.println("Fetching projects XML, page=" + page);
            long start = currentTimeMillis();
            HTTPRequest request = new HTTPRequest(new URI(projectsUri + "?page=" + page), GET);
            HTTPResponse response = httpCache.execute(request);
            long end = currentTimeMillis();
            System.out.println("Fetched XML in " + (end - start) + "ms.");

            byte[] bytes = IOUtils.toByteArray(response.getPayload().getInputStream());
            try {
                Document doc = xmlReader.readDocument(new ByteArrayInputStream(bytes));

                List<GitoriousProject> list = GitoriousProject.projectsFromXml(gitoriousUrl, doc.getRootElement());

                // This indicates the last page.
                if (list.size() == 0) {
                    break;
                }

                System.out.println("Parsed out " + list.size() + " projects.");
                all.addAll(list);
            } catch (XMLStreamException e) {
                System.out.println("Unable to parse XML.");
                System.out.println(new String(bytes));
            }

            page++;
        }

        return all;
    }
}

package io.trygvis.esper.testing.gitorious;

import static java.lang.System.*;

import fj.data.Option;
import org.apache.abdera.parser.ParseException;
import org.apache.commons.io.*;
import static org.apache.commons.lang.StringUtils.*;
import static org.codehaus.httpcache4j.HTTPMethod.*;

import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.dom4j.*;
import org.dom4j.io.*;

import javax.xml.stream.*;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.*;

public class GitoriousClient {
    public static final STAXEventReader xmlReader = new STAXEventReader();
    public final String baseUrl;
    private final HTTPCache http;
    private final String projectsUri;
    private final GitoriousAtomFeedParser parser = new GitoriousAtomFeedParser();

    public GitoriousClient(HTTPCache http, String baseUrl) throws URISyntaxException {
        this.http = http;
        this.baseUrl = new URI(baseUrl).toASCIIString();
        this.projectsUri = baseUrl + "/projects.xml";
    }

    public Set<GitoriousProjectXml> findProjects() throws Exception {
        System.out.println("Fetching all projects");
        int page = 1;

        Set<GitoriousProjectXml> all = new HashSet<>();
        while (true) {
            System.out.println("Fetching projects, page=" + page);
            long start = currentTimeMillis();
            HTTPResponse response = http.execute(new HTTPRequest(new URI(projectsUri + "?page=" + page), GET));
            long end = currentTimeMillis();
            System.out.println("Fetched in " + (end - start) + "ms.");

            if (!response.getStatus().equals(Status.OK)) {
                System.out.println("Got non-200 status from server: " + response.getStatus());
                break;
            }

            MIMEType mimeType = MIMEType.valueOf(trimToEmpty(response.getHeaders().getFirstHeaderValue("Content-Type")));
            if (!mimeType.getPrimaryType().equals("application") || !mimeType.getSubType().equals("xml")) {
                System.out.println("Unexpected mime type, probably at the end of the list: " + mimeType);
                break;
            }

            byte[] bytes = IOUtils.toByteArray(response.getPayload().getInputStream());
            try {
                Document doc = xmlReader.readDocument(new ByteArrayInputStream(bytes));

                List<GitoriousProjectXml> list = GitoriousProjectXml.projectsFromXml(doc.getRootElement());

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

    public URI atomFeed(String projectSlug, String repositoryName) {
        return URI.create(baseUrl + "/" + projectSlug + "/" + repositoryName + ".atom");
    }

    public Iterable<GitoriousEvent> fetchGitoriousEvents(GitoriousRepository repository, Option<Date> lastUpdate) throws SQLException, ParseException {
        System.out.println("Fetching " + repository.atomFeed);

        long start = currentTimeMillis();
        HTTPResponse response = http.execute(new HTTPRequest(repository.atomFeed, HTTPMethod.GET));
        long end = currentTimeMillis();
        System.out.println("Fetched in " + (end - start) + "ms");

        // Use the server's timestamp
        Date responseDate = response.getDate().toDate();

        System.out.println("responseDate = " + responseDate);

        return parser.parseStream(response.getPayload().getInputStream(), lastUpdate, repository.projectSlug, repository.name);
    }
}

class GitoriousProjectXml implements Comparable<GitoriousProjectXml> {
    public final String slug;
    public final List<GitoriousRepositoryXml> repositories;

    public GitoriousProjectXml(String slug, List<GitoriousRepositoryXml> repositories) {
        this.slug = slug;
        this.repositories = repositories;
    }

    public static GitoriousProjectXml fromXml(Element project) throws URISyntaxException {
        String slug = trimToNull(project.elementText("slug"));

        if (slug == null) {
            System.out.println("Missing slug");
            return null;
        }

        Element repositories = project.element("repositories");
        if (repositories == null) {
            System.out.println("Missing <repositories>");
            return null;
        }

        Element mainlines = repositories.element("mainlines");
        if (mainlines == null) {
            System.out.println("Missing <mainlines>");
            return null;
        }

        @SuppressWarnings("unchecked") List<Element> list = (List<Element>) mainlines.elements("repository");
        List<GitoriousRepositoryXml> repositoryList = new ArrayList<>(list.size());
        for (Element repository : list) {
            GitoriousRepositoryXml r = GitoriousRepositoryXml.fromXml(slug, repository);

            if (r == null) {
                continue;
            }

            repositoryList.add(r);
        }

        return new GitoriousProjectXml(slug, repositoryList);
    }

    public static List<GitoriousProjectXml> projectsFromXml(Element root) throws URISyntaxException {
        List<GitoriousProjectXml> projects = new ArrayList<>();
        @SuppressWarnings("unchecked") List<Element> elements = (List<Element>) root.elements("project");
        for (Element project : elements) {

            GitoriousProjectXml p = GitoriousProjectXml.fromXml(project);
            if (p == null) {
                System.out.println(project.toString());
                continue;
            }
            projects.add(p);
        }

        return projects;
    }

    public int compareTo(GitoriousProjectXml other) {
        return slug.compareTo(other.slug);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitoriousProjectXml)) return false;

        GitoriousProjectXml that = (GitoriousProjectXml) o;

        return repositories.equals(that.repositories) && slug.equals(that.slug);
    }

    public int hashCode() {
        int result = slug.hashCode();
        result = 31 * result + repositories.hashCode();
        return result;
    }
}

class GitoriousRepositoryXml implements Comparable<GitoriousRepositoryXml> {
    public final String projectSlug;
    public final String name;

    GitoriousRepositoryXml(String projectSlug, String name) {
        this.projectSlug = projectSlug;
        this.name = name;
    }

    public static GitoriousRepositoryXml fromXml(String project, Element element) throws URISyntaxException {
        String name = trimToNull(element.elementText("name"));

        if (name == null) {
            return null;
        }

        return new GitoriousRepositoryXml(project, name);
    }

    public int compareTo(GitoriousRepositoryXml o) {
        int a = projectSlug.compareTo(o.projectSlug);

        if (a != 0) {
            return a;
        }

        return name.compareTo(o.name);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitoriousRepositoryXml)) return false;

        GitoriousRepositoryXml that = (GitoriousRepositoryXml) o;

        return name.equals(that.name) && projectSlug.equals(that.projectSlug);
    }

    public int hashCode() {
        int result = projectSlug.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}

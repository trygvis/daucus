package io.trygvis.esper.testing.gitorious;

import fj.*;
import fj.data.*;
import io.trygvis.esper.testing.util.*;
import org.apache.abdera.parser.*;
import org.apache.commons.io.*;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.cache.*;
import org.dom4j.*;
import org.dom4j.io.*;

import javax.xml.stream.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fj.data.Option.*;
import static org.apache.commons.lang.StringUtils.*;

public class GitoriousClient {
    public static final STAXEventReader xmlReader = new STAXEventReader();
    public final String baseUrl;
    private final HttpClient<List<GitoriousProjectXml>> http;
    private final String projectsUri;
    private final GitoriousAtomFeedParser parser;

    private final F<HTTPResponse, Option<List<GitoriousProjectXml>>> parseDocument = new F<HTTPResponse, Option<List<GitoriousProjectXml>>>() {
        @Override
        public Option<List<GitoriousProjectXml>> f(HTTPResponse response) {
            MIMEType mimeType = MIMEType.valueOf(trimToEmpty(response.getHeaders().getFirstHeaderValue("Content-Type")));
            if (!mimeType.getPrimaryType().equals("application") || !mimeType.getSubType().equals("xml")) {
                System.out.println("Unexpected mime type, probably at the end of the list: " + mimeType);
                return none();
            }

            byte[] bytes;

            try {
                bytes = IOUtils.toByteArray(response.getPayload().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return none();
            }

            try {
                Document doc = xmlReader.readDocument(new ByteArrayInputStream(bytes));

                List<GitoriousProjectXml> list = GitoriousProjectXml.projectsFromXml(doc.getRootElement());

                System.out.println("Parsed out " + list.size() + " projects.");

                return some(list);
            } catch (XMLStreamException e) {
                System.out.println("Unable to parse XML.");
                System.out.println(new String(bytes));
                return none();
            }
        }
    };

    public GitoriousClient(HTTPCache cache, String baseUrl, GitoriousAtomFeedParser parser) throws URISyntaxException {
        this.http = new HttpClient<>(cache, parseDocument);
        this.baseUrl = new URI(baseUrl).toASCIIString();
        this.parser = parser;
        this.projectsUri = baseUrl + "/projects.xml";
    }

    public Set<GitoriousProjectXml> findProjects() throws Exception {
        System.out.println("Fetching all projects");
        int page = 1;

        Set<GitoriousProjectXml> all = new HashSet<>();
        while (true) {
            Option<P2<List<GitoriousProjectXml>, byte[]>> option = http.fetch(new URI(projectsUri + "?page=" + page));

            if (option.isNone()) {
                return all;
            }

            List<GitoriousProjectXml> list = option.some()._1();

            // TODO: store data

            // This indicates the last page.
            if (list.size() == 0) {
                break;
            }

            all.addAll(list);

            page++;
        }

        return all;
    }

    public URI atomFeed(String projectSlug, String repositoryName) {
        return URI.create(baseUrl + "/" + projectSlug + "/" + repositoryName + ".atom");
    }

    public Iterable<GitoriousEvent> fetchGitoriousEvents(GitoriousRepositoryDto repository, Option<Date> lastUpdate) throws SQLException, ParseException {
        throw new RuntimeException("re-implement");
//        System.out.println("Fetching " + repository.atomFeed);
//
//        long start = currentTimeMillis();
//        HTTPResponse response = http.execute(new HTTPRequest(repository.atomFeed, HTTPMethod.GET));
//        long end = currentTimeMillis();
//        System.out.println("Fetched in " + (end - start) + "ms");
//
//        // Use the server's timestamp
//        Date responseDate = response.getDate().toDate();
//
//        System.out.println("responseDate = " + responseDate);
//
//        return parser.parseStream(response.getPayload().getInputStream(), lastUpdate, repository.projectSlug, repository.name);
    }
}

class GitoriousProjectXml implements Comparable<GitoriousProjectXml> {
    public final String slug;
    public final List<GitoriousRepositoryXml> repositories;

    public GitoriousProjectXml(String slug, List<GitoriousRepositoryXml> repositories) {
        this.slug = slug;
        this.repositories = repositories;
    }

    public static GitoriousProjectXml fromXml(Element project) {
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

    public static List<GitoriousProjectXml> projectsFromXml(Element root) {
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

    public static GitoriousRepositoryXml fromXml(String project, Element element) {
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

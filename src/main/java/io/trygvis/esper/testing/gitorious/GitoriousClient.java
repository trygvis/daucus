package io.trygvis.esper.testing.gitorious;

import static java.lang.System.*;
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
import java.util.*;

public class GitoriousClient {
    public static final STAXEventReader xmlReader = new STAXEventReader();
    private final HTTPCache httpCache;
    public final String baseUrl;
    private final String projectsUri;

    public GitoriousClient(HTTPCache httpCache, String baseUrl) throws URISyntaxException {
        this.httpCache = httpCache;
        this.baseUrl = new URI(baseUrl).toASCIIString();
        this.projectsUri = baseUrl + "/projects.xml";
    }

    public Set<GitoriousProjectXml> findProjects() throws Exception {
        System.out.println("Fetching all projects");
        int page = 1;

        Set<GitoriousProjectXml> all = new HashSet<>();
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

    public URI atomFeed(String slug) {
        return URI.create(baseUrl + "/" + slug + ".atom");
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

        List<Element> list = (List<Element>) mainlines.elements("repository");
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
        for (Element project : (List<Element>) root.elements("project")) {

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

        if (!repositories.equals(that.repositories)) return false;
        if (!slug.equals(that.slug)) return false;

        return true;
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

        if (!name.equals(that.name)) return false;
        if (!projectSlug.equals(that.projectSlug)) return false;

        return true;
    }

    public int hashCode() {
        int result = projectSlug.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}

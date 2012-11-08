package io.trygvis.esper.testing.gitorious;

import static org.apache.commons.lang.StringUtils.*;
import org.dom4j.*;

import java.net.*;
import java.util.*;

public class GitoriousProject implements Comparable<GitoriousProject> {
    public final String slug;
    public final List<GitoriousRepository> repositories;

    public GitoriousProject(String slug, List<GitoriousRepository> repositories) {
        this.slug = slug;
        this.repositories = repositories;
    }

    public static GitoriousProject fromXml(String gitoriousUrl, Element project) throws URISyntaxException {
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
        List<GitoriousRepository> repositoryList = new ArrayList<>(list.size());
        for (Element repository : list) {
            GitoriousRepository r = GitoriousRepository.fromXml(gitoriousUrl, slug, repository);

            if (r == null) {
                continue;
            }

            repositoryList.add(r);
        }

        return new GitoriousProject(slug, repositoryList);
    }

    public static List<GitoriousProject> projectsFromXml(String gitoriousUrl, Element root) throws URISyntaxException {
        List<GitoriousProject> projects = new ArrayList<>();
        for (Element project : (List<Element>) root.elements("project")) {

            GitoriousProject p = GitoriousProject.fromXml(gitoriousUrl, project);
            if (p == null) {
                System.out.println(project.toString());
                continue;
            }
            projects.add(p);
        }

        return projects;
    }

    public int compareTo(GitoriousProject other) {
        return slug.compareTo(other.slug);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitoriousProject)) return false;

        GitoriousProject that = (GitoriousProject) o;

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

class GitoriousRepository implements Comparable<GitoriousRepository> {
    public final String projectSlug;
    public final String name;
    public final URI atom;

    GitoriousRepository(String projectSlug, String name, URI atom) {
        this.projectSlug = projectSlug;
        this.name = name;
        this.atom = atom;
    }

    public static GitoriousRepository fromXml(String gitoriousUrl, String project, Element element) throws URISyntaxException {
        String name = trimToNull(element.elementText("name"));

        if (name == null) {
            return null;
        }

        return new GitoriousRepository(project, name, new URI(gitoriousUrl + "/" + project + "/" + name + ".atom"));
    }

    public int compareTo(GitoriousRepository o) {
        int a = projectSlug.compareTo(o.projectSlug);

        if (a != 0) {
            return a;
        }

        return name.compareTo(o.name);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitoriousRepository)) return false;

        GitoriousRepository that = (GitoriousRepository) o;

        if (!atom.equals(that.atom)) return false;
        if (!name.equals(that.name)) return false;
        if (!projectSlug.equals(that.projectSlug)) return false;

        return true;
    }

    public int hashCode() {
        int result = projectSlug.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + atom.hashCode();
        return result;
    }
}

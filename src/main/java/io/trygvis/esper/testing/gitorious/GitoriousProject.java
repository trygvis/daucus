package io.trygvis.esper.testing.gitorious;

import fj.data.*;

import java.net.*;
import java.util.*;

public class GitoriousProject implements Comparable<GitoriousProject> {
    public final String slug;

    public GitoriousProject(String slug) {
        this.slug = slug;
    }

    public int compareTo(GitoriousProject other) {
        return slug.compareTo(other.slug);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitoriousProject)) return false;

        GitoriousProject that = (GitoriousProject) o;

        if (!slug.equals(that.slug)) return false;

        return true;
    }

    public int hashCode() {
        int result = slug.hashCode();
        return result;
    }
}

class GitoriousRepository implements Comparable<GitoriousRepository> {
    public final String projectSlug;
    public final String name;
    public final URI atomFeed;
    public final Option<Date> lastUpdate;
    public final Option<Date> lastSuccessfulUpdate;

    GitoriousRepository(String projectSlug, String name, URI atomFeed, Option<Date> lastUpdate, Option<Date> lastSuccessfulUpdate) {
        this.projectSlug = projectSlug;
        this.name = name;
        this.atomFeed = atomFeed;
        this.lastUpdate = lastUpdate;
        this.lastSuccessfulUpdate = lastSuccessfulUpdate;
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

package io.trygvis.esper.testing.gitorious;

import fj.data.*;

import java.net.*;
import java.util.*;

public class GitoriousProjectDto implements Comparable<GitoriousProjectDto> {
    public final String slug;

    public GitoriousProjectDto(String slug) {
        this.slug = slug;
    }

    public int compareTo(GitoriousProjectDto other) {
        return slug.compareTo(other.slug);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitoriousProjectDto)) return false;

        GitoriousProjectDto that = (GitoriousProjectDto) o;

        return slug.equals(that.slug);
    }

    public int hashCode() {
        return slug.hashCode();
    }
}

class GitoriousRepositoryDto implements Comparable<GitoriousRepositoryDto> {
    public final String projectSlug;
    public final String name;
    public final URI atomFeed;
    public final Option<Date> lastUpdate;
    public final Option<Date> lastSuccessfulUpdate;

    GitoriousRepositoryDto(String projectSlug, String name, URI atomFeed, Option<Date> lastUpdate, Option<Date> lastSuccessfulUpdate) {
        this.projectSlug = projectSlug;
        this.name = name;
        this.atomFeed = atomFeed;
        this.lastUpdate = lastUpdate;
        this.lastSuccessfulUpdate = lastSuccessfulUpdate;
    }

    public int compareTo(GitoriousRepositoryDto o) {
        int a = projectSlug.compareTo(o.projectSlug);

        if (a != 0) {
            return a;
        }

        return name.compareTo(o.name);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitoriousRepositoryDto)) return false;

        GitoriousRepositoryDto that = (GitoriousRepositoryDto) o;

        return name.equals(that.name) && projectSlug.equals(that.projectSlug);
    }

    public int hashCode() {
        return 31 * projectSlug.hashCode() + name.hashCode();
    }
}

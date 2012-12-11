package io.trygvis.esper.testing.nexus;

import fj.data.*;

import java.util.*;

public class ArtifactDto implements Comparable<ArtifactDto> {
    public final UUID uuid;
    public final String repositoryId;
    public final ArtifactId id;

    public ArtifactDto(UUID uuid, String repositoryId, ArtifactId id) {
        this.uuid = uuid;
        this.repositoryId = repositoryId;
        this.id = id;
    }

    public int compareTo(ArtifactDto o) {
        return id.compareTo(o.id);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactDto)) return false;

        ArtifactDto that = (ArtifactDto) o;

        return uuid.equals(that.uuid);
    }

    public int hashCode() {
        return uuid.hashCode();
    }
}

class ArtifactFile {
    public final Option<String> classifier;
    public final String extension;

    ArtifactFile(Option<String> classifier, String extension) {
        this.classifier = classifier;
        this.extension = extension;
    }
}

class ArtifactId implements Comparable<ArtifactId> {
    public final String groupId;
    public final String artifactId;
    public final String version;

    ArtifactId(String groupId, String artifactId, String version) {
        if(groupId == null) {
            throw new NullPointerException("groupId");
        }
        if(artifactId == null) {
            throw new NullPointerException("artifactId");
        }
        if(version == null) {
            throw new NullPointerException("version");
        }
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public int compareTo(ArtifactId o) {
        int i = groupId.compareTo(o.groupId);

        if (i != 0) {
            return i;
        }

        i = artifactId.compareTo(o.artifactId);

        if (i != 0) {
            return i;
        }

        return version.compareTo(o.version);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactId)) return false;

        ArtifactId that = (ArtifactId) o;

        return groupId.equals(that.groupId) &&
            artifactId.equals(that.artifactId) &&
            version.equals(that.version);
    }

    public int hashCode() {
        int result = groupId != null ? groupId.hashCode() : 0;
        result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}

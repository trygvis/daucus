package io.trygvis.esper.testing.nexus;

import com.google.common.base.*;
import com.google.common.collect.*;
import fj.*;
import fj.data.*;
import static fj.data.Option.fromNull;
import static org.apache.commons.lang.StringUtils.*;
import org.dom4j.*;
import org.dom4j.io.*;

import java.io.*;
import java.util.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.*;

public class NexusParser {
    public static final STAXEventReader xmlReader = new STAXEventReader();

    public static ArtifactSearchResult parseDocument(InputStream is) throws XMLStreamException {
        Document doc = xmlReader.readDocument(is);

        Option<Integer> totalCount = fromNull(trimToNull(doc.getRootElement().elementText("totalCount"))).
                bind(Option.parseInt);
        if (totalCount.isNone()) {
            throw new RuntimeException("Could not find required element <totalCount>");
        }

        boolean tooManyResults = "true".equals(trimToNull(doc.getRootElement().elementText("tooManyResults")));

        List<ArtifactXml> list = new ArrayList<>();
        for (Object o : doc.selectNodes("/searchNGResponse/data/artifact")) {
            if (!(o instanceof Element)) {
                continue;
            }

            Element artifact = (Element) o;

            String groupId = trimToNull(artifact.elementText("groupId"));
            String artifactId = trimToNull(artifact.elementText("artifactId"));
            String version = trimToNull(artifact.elementText("version"));

            if (groupId == null || artifactId == null || version == null) {
                continue;
            }

            List<ArtifactHits> artifactHitsList = new ArrayList<>();

            @SuppressWarnings("unchecked") List<Element> artifactHits = (List<Element>) artifact.selectNodes("artifactHits/artifactHit");
            for (Element artifactHit : artifactHits) {
                String repositoryId = trimToNull(artifactHit.elementText("repositoryId"));
                if (repositoryId == null) {
                    continue;
                }
                List<ArtifactFile> files = new ArrayList<>();

                @SuppressWarnings("unchecked") List<Element> artifactLinks = artifactHit.selectNodes("artifactLinks/artifactLink");
                for (Element artifactLink : artifactLinks) {
                    Option<String> classifier = Option.fromString(trimToEmpty(artifactLink.elementText("classifier")));
                    String extension = trimToNull(artifactLink.elementText("extension"));

                    if (extension == null) {
                        continue;
                    }

                    files.add(new ArtifactFile(classifier, extension));
                }

                artifactHitsList.add(new ArtifactHits(repositoryId, files));
            }

            list.add(new ArtifactXml(groupId, artifactId, version, artifactHitsList));
        }

        return new ArtifactSearchResult(totalCount.some(), tooManyResults, list);
    }
}

class ArtifactSearchResult {
    public final int totalCount;
    public final boolean tooManyResults;
    public final List<ArtifactXml> artifacts;

    ArtifactSearchResult(int totalCount, boolean tooManyResults, List<ArtifactXml> artifacts) {
        this.totalCount = totalCount;
        this.tooManyResults = tooManyResults;
        this.artifacts = artifacts;
    }

    public ArtifactSearchResult append(ArtifactSearchResult result) {
        List<ArtifactXml> list = Lists.newArrayList(artifacts);
        list.addAll(result.artifacts);
        return new ArtifactSearchResult(result.totalCount, result.tooManyResults, list);
    }
}

class ArtifactXml implements Comparable<ArtifactXml> {
    public final String groupId;
    public final String artifactId;
    public final String version;
    public final List<ArtifactHits> hits;

    ArtifactXml(String groupId, String artifactId, String version, List<ArtifactHits> hits) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.hits = hits;
    }

    public static Predicate<ArtifactXml> repositoryFilter(final String repositoryId) {
        return new Predicate<ArtifactXml>() {
            public boolean apply(ArtifactXml artifact) {
                return Iterables.any(artifact.hits, new Predicate<ArtifactHits>() {
                    public boolean apply(ArtifactHits hits) {
                        return hits.repositoryId.equals(repositoryId);
                    }
                });
            }
        };
    }

    public FlatArtifact flatten(String repositoryId) {
        for (ArtifactHits hit : hits) {
            if (hit.repositoryId.equals(repositoryId)) {
                return new FlatArtifact(groupId, artifactId, version, hit.files);
            }
        }

        throw new RuntimeException("No hits in repository " + repositoryId);
    }

    public int compareTo(ArtifactXml o) {
        int c = groupId.compareTo(o.groupId);

        if(c != 0) {
            return c;
        }

        c = artifactId.compareTo(o.artifactId);

        if(c != 0) {
            return c;
        }

        return version.compareTo(o.version);
    }

    public String getId() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public Set<String> repositories() {
        Set<String> repositories = new HashSet<>(10);

        for (ArtifactHits hit : hits) {
            repositories.add(hit.repositoryId);
        }

        return repositories;
    }
}

class FlatArtifact {
    public final String groupId;
    public final String artifactId;
    public final String version;
    public final List<ArtifactFile> files;

    FlatArtifact(String groupId, String artifactId, String version, List<ArtifactFile> files) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.files = files;
    }
}

class ArtifactHits {
    public final String repositoryId;
    public final List<ArtifactFile> files;

    ArtifactHits(String repositoryId, List<ArtifactFile> files) {
        this.repositoryId = repositoryId;
        this.files = files;
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

package io.trygvis.esper.testing.nexus;

import com.google.common.collect.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import org.apache.commons.lang.*;

import java.io.*;
import java.util.*;

public class NexusImporter {
    public static void main(String[] args) throws IOException {
        Config config = Config.loadFromDisk();

        NexusClient client = new NexusClient(HttpClient.createHttpClient(config), config.nexusUrl);

        ArtifactSearchResult result = client.fetchIndex("eu.nets", Option.<String>none());
        ArrayList<ArtifactXml> artifacts = Lists.newArrayList(result.artifacts);
        Collections.sort(artifacts);
        for (ArtifactXml artifact : artifacts) {
            System.out.println("repo=" + StringUtils.join(artifact.repositories(), ", ") + ", artifact=" + artifact.getId());
        }
    }
}

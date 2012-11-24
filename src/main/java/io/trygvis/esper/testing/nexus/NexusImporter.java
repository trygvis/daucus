package io.trygvis.esper.testing.nexus;

import com.jolbox.bonecp.*;
import fj.data.*;
import static fj.data.Option.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.object.*;
import static java.lang.Thread.*;
import org.codehaus.httpcache4j.cache.*;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.*;

public class NexusImporter {
    public static void main(String[] args) throws Exception {
        final Config config = Config.loadFromDisk();

        final HTTPCache http = HttpClient.createHttpClient(config);

        final BoneCPDataSource boneCp = config.createBoneCp();

        ObjectManager<NexusServerDto, ActorRef<NexusServer>> serverManager = new ObjectManager<>("Nexus server", Collections.<NexusServerDto>emptySet(), new ObjectFactory<NexusServerDto, ActorRef<NexusServer>>() {
            public ActorRef<NexusServer> create(NexusServerDto server) {
                final NexusClient client = new NexusClient(http, server.url);

                String name = server.name;

                return ObjectUtil.threadedActor(boneCp, "", config.nexusUpdateInterval, new NexusServer(client, server));
            }
        });

        final AtomicBoolean shouldRun = new AtomicBoolean(true);
        config.addShutdownHook(currentThread(), shouldRun);

        while (shouldRun.get()) {
            try {
                try (Connection c = boneCp.getConnection()) {
                    serverManager.update(new NexusDao(c).selectServer());
                }
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }

            synchronized (shouldRun) {
                shouldRun.wait(60 * 1000);
            }
        }

        serverManager.close();
    }
}

class NexusServer implements TransactionalActor {

    public final NexusClient client;
    public final NexusServerDto server;

    NexusServer(NexusClient client, NexusServerDto server) {
        this.client = client;
        this.server = server;
    }

    public void act(Connection c) throws Exception {
        Date timestamp = new Date();
        NexusDao dao = new NexusDao(c);

        String p = server.name;

        for (NexusRepositoryDto repository : dao.findRepositories(server.url)) {
            String p2 = p + "/" + repository.repositoryId;

            System.out.println(p2 + ": Updating repository: " + repository.repositoryId);

            TreeMap<ArtifactId, ArtifactXml> artifactsInNexus = new TreeMap<>();

            for (String groupId : repository.groupIds) {
                String p3 = p2 + "/" + groupId;

                System.out.println(p3 + ": Updating group id");
                ArtifactSearchResult result = client.fetchIndex(groupId, some(repository.repositoryId));
                System.out.println(p3 + ": Found " + result.artifacts.size() + " artifacts");

                for (ArtifactXml xml : result.artifacts) {
                    artifactsInNexus.put(xml.id, xml);
                }

                System.out.println(p3 + ": Updating everything under group id");
                result = client.fetchIndex(groupId + ".*", some(repository.repositoryId));
                System.out.println(p3 + ": Found " + result.artifacts.size() + " artifacts");

                for (ArtifactXml xml : result.artifacts) {
                    artifactsInNexus.put(xml.id, xml);
                }
            }

            Map<ArtifactId, ArtifactDto> artifactsInDatabase = new HashMap<>();
            for (ArtifactDto dto : dao.findArtifactsInRepository(server.url, repository.repositoryId)) {
                artifactsInDatabase.put(dto.id, dto);
            }

            ArrayList<FlatArtifact> created = new ArrayList<>();
            ArrayList<FlatArtifact> kept = new ArrayList<>();
            ArrayList<ArtifactDto> removed = new ArrayList<>();

            for (ArtifactXml xml : artifactsInNexus.values()) {
                Option<FlatArtifact> o = xml.flatten(repository.repositoryId);

                if(o.isNone()) {
                    continue;
                }

                FlatArtifact artifact = o.some();

                if(!artifactsInDatabase.containsKey(xml.id)) {
                    created.add(artifact);
                }
                else {
                    kept.add(artifact);
                }
            }

            for (ArtifactDto dto : artifactsInDatabase.values()) {
                if(!artifactsInNexus.containsKey(dto.id)) {
                    removed.add(dto);
                }
            }

            System.out.println(p2 + ": found " + created.size() + " new artifacts, " + removed.size() + " removed artifacts and " + kept.size() + " existing artifacts.");

            System.out.println(p2 + ": inserting new artifacts");
            for (FlatArtifact artifact : created) {
                dao.insertArtifact(repository.nexusUrl, repository.repositoryId, artifact.id, Option.<String>none(), artifact.files, timestamp);
            }
            System.out.println(p2 + ": inserted");

            System.out.println(p2 + ": deleting removed artifacts");
            for (ArtifactDto artifact : removed) {
                dao.deleteArtifact(repository.nexusUrl, repository.repositoryId, artifact.id);
            }
            System.out.println(p2 + ": deleted");
        }

        c.commit();
    }
}

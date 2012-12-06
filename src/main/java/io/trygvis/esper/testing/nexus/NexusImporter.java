package io.trygvis.esper.testing.nexus;

import com.jolbox.bonecp.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.object.*;
import io.trygvis.esper.testing.util.*;
import static java.lang.Thread.*;
import static java.util.regex.Pattern.quote;
import org.codehaus.httpcache4j.cache.*;

import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.*;

public class NexusImporter {
    public static void main(String[] args) throws Exception {
        final Config config = Config.loadFromDisk();
        final HTTPCache http = HttpClient.createHttpCache(config);
        final XmlParser xmlParser = new XmlParser();
        final BoneCPDataSource boneCp = config.createBoneCp();

        XmlParser.debugXml = false;

        ObjectManager<NexusServerDto, ActorRef<NexusServer>> serverManager = new ObjectManager<>("Nexus server", Collections.<NexusServerDto>emptySet(), new ObjectFactory<NexusServerDto, ActorRef<NexusServer>>() {
            public ActorRef<NexusServer> create(NexusServerDto server) {
                final NexusClient client = new NexusClient(http, server.url);

                String name = server.name;

                return ObjectUtil.threadedActor(name, config.nexusUpdateInterval, boneCp, "Nexus Server: " + name, new NexusServer(client, server, xmlParser));
            }
        });

        final AtomicBoolean shouldRun = new AtomicBoolean(true);
        config.addShutdownHook(currentThread(), shouldRun);

        while (shouldRun.get()) {
            try {
                List<NexusServerDto> newKeys;

                try (Connection c = boneCp.getConnection()) {
                    newKeys = new NexusDao(c).selectServer();
                }

                serverManager.update(newKeys);
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
    public final XmlParser xmlParser;

    NexusServer(NexusClient client, NexusServerDto server, XmlParser xmlParser) {
        this.client = client;
        this.server = server;
        this.xmlParser = xmlParser;
    }

    @Override
    public void act(Connection c) throws Exception {

        NexusDao dao = new NexusDao(c);

        NexusFeed feed = client.fetchTimeline("recentlyDeployedArtifacts");

        int newEvents = 0, oldEvents = 0;

        for (NexusEvent event : feed.events) {

            if(dao.countEventByGuid(event.guid) != 0) {
                oldEvents++;
                continue;
            }

            newEvents++;

            String repositoryId = event.guid.replaceAll("^" + quote(server.url.toASCIIString()) + "/content/repositories/([-a-zA-Z0-9]*)/.*", "$1");

            if(repositoryId.length() == 0) {
                continue;
            }

            Option<NexusRepositoryDto> r = dao.findRepository(server.uuid, repositoryId);

            if(r.isNone()) {
                continue;
            }

            NexusRepositoryDto repository = r.some();

            Option<ArtifactDto> a = dao.findArtifact(repository.uuid, event.artifactId);

            UUID uuid;

            if(a.isNone()) {
                System.out.println("New artifact: " + event.artifactId);
                uuid = dao.insertArtifact(repository.uuid, event.artifactId);
            }
            else {
                ArtifactDto artifact = a.some();

                System.out.println("Updated artifact: " + event.artifactId);
//                    dao.updateSnapshotTimestamp(artifact.uuid, newSnapshotEvent.snapshotTimestamp);

                uuid = artifact.uuid;
            }

            if (event instanceof NewSnapshotEvent) {
                NewSnapshotEvent newSnapshotEvent = (NewSnapshotEvent) event;

                dao.insertNewSnapshotEvent(uuid, event.guid, newSnapshotEvent.snapshotTimestamp,
                        newSnapshotEvent.buildNumber, newSnapshotEvent.url.toASCIIString());
            } else if (event instanceof NewReleaseEvent) {
                NewReleaseEvent nre = (NewReleaseEvent) event;

                dao.insertNewReleaseEvent(uuid, event.guid, nre.url.toASCIIString());
            } else {
                System.out.println("Unknown event type: " + event.getClass().getName());
            }
        }

        System.out.println("Timeline updated. New=" + newEvents + ", old=" + oldEvents);
    }

//    public void act(Connection c) throws Exception {
//        Date timestamp = new Date();
//        NexusDao dao = new NexusDao(c);
//
//        String p = server.name;
//
//        for (NexusRepositoryDto repository : dao.findRepositories(server.url)) {
//            String p2 = p + "/" + repository.repositoryId;
//
//            System.out.println(p2 + ": Updating repository: " + repository.repositoryId);
//
//            TreeMap<ArtifactId, ArtifactXml> artifactsInNexus = new TreeMap<>();
//
//            for (String groupId : repository.groupIds) {
//                String p3 = p2 + "/" + groupId;
//
//                System.out.println(p3 + ": Updating group id");
//                ArtifactSearchResult result = client.fetchIndex(groupId, some(repository.repositoryId));
//                System.out.println(p3 + ": Found " + result.artifacts.size() + " artifacts");
//
//                for (ArtifactXml xml : result.artifacts) {
//                    artifactsInNexus.put(xml.id, xml);
//                }
//
//                System.out.println(p3 + ": Updating everything under group id");
//                result = client.fetchIndex(groupId + ".*", some(repository.repositoryId));
//                System.out.println(p3 + ": Found " + result.artifacts.size() + " artifacts");
//
//                for (ArtifactXml xml : result.artifacts) {
//                    artifactsInNexus.put(xml.id, xml);
//                }
//            }
//
//            Map<ArtifactId, ArtifactDto> artifactsInDatabase = new HashMap<>();
//            for (ArtifactDto dto : dao.findArtifactsInRepository(server.url, repository.repositoryId)) {
//                artifactsInDatabase.put(dto.id, dto);
//            }
//
//            ArrayList<FlatArtifact> created = new ArrayList<>();
//            ArrayList<FlatArtifact> kept = new ArrayList<>();
//            ArrayList<ArtifactDto> removed = new ArrayList<>();
//
//            for (ArtifactXml xml : artifactsInNexus.values()) {
//                Option<FlatArtifact> o = xml.flatten(repository.repositoryId);
//
//                if(o.isNone()) {
//                    continue;
//                }
//
//                FlatArtifact artifact = o.some();
//
//                if(!artifactsInDatabase.containsKey(xml.id)) {
//                    created.add(artifact);
//                }
//                else {
//                    kept.add(artifact);
//                }
//            }
//
//            for (ArtifactDto dto : artifactsInDatabase.values()) {
//                if(!artifactsInNexus.containsKey(dto.id)) {
//                    removed.add(dto);
//                }
//            }
//
//            System.out.println(p2 + ": found " + created.size() + " new artifacts, " + removed.size() + " removed artifacts and " + kept.size() + " existing artifacts.");
//
//            System.out.println(p2 + ": inserting new artifacts");
//            for (FlatArtifact artifact : created) {
//                dao.insertArtifact(repository.nexusUrl, repository.repositoryId, artifact.id, Option.<String>none(), artifact.files, timestamp);
//            }
//            System.out.println(p2 + ": inserted");
//
//            System.out.println(p2 + ": deleting removed artifacts");
//            for (ArtifactDto artifact : removed) {
//                dao.deleteArtifact(repository.nexusUrl, repository.repositoryId, artifact.id);
//            }
//            System.out.println(p2 + ": deleted");
//        }
//
//        c.commit();
//    }
}

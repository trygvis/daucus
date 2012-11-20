package io.trygvis.esper.testing.nexus;

import com.google.common.collect.*;
import com.jolbox.bonecp.*;
import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.object.*;
import static java.lang.Thread.*;
import org.apache.commons.lang.*;
import org.codehaus.httpcache4j.cache.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class NexusImporter {
    public static void main(String[] args) throws Exception {
        final Config config = Config.loadFromDisk();

        final HTTPCache http = HttpClient.createHttpClient(config);

        final BoneCPDataSource boneCp = config.createBoneCp();

        ObjectManager<NexusServerDto, ActorRef<NexusServer>> serverManager = new ObjectManager<>("Nexus server", Collections.<NexusServerDto>emptySet(), new ObjectFactory<NexusServerDto, ActorRef<NexusServer>>() {
            public ActorRef<NexusServer> create(NexusServerDto server) {
                final NexusClient client = new NexusClient(http, server.url);

                return ObjectUtil.threadedActor(boneCp, config.nexusUpdateInterval, new NexusServer(client, server));
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
                shouldRun.wait(1000);
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
        NexusDao dao = new NexusDao(c);

        for (NexusRepositoryDto repository : dao.findRepositories(server.url)) {
            System.out.println("Updating repository: " + repository.repositoryId);
            for (String groupId : repository.groupIds) {
                System.out.println("Updating groupId: " + groupId);
                ArtifactSearchResult result = client.fetchIndex(groupId, Option.<String>none());

                ArrayList<ArtifactXml> artifacts = Lists.newArrayList(result.artifacts);
                Collections.sort(artifacts);
                for (ArtifactXml artifact : artifacts) {
                    System.out.println("repo=" + StringUtils.join(artifact.repositories(), ", ") + ", artifact=" + artifact.getId());
                }
            }
        }

        c.commit();
    }
}

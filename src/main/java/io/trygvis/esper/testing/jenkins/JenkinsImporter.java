package io.trygvis.esper.testing.jenkins;

import com.jolbox.bonecp.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.object.*;
import io.trygvis.esper.testing.util.*;
import org.apache.abdera.*;
import org.codehaus.httpcache4j.cache.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static io.trygvis.esper.testing.object.ObjectUtil.*;
import static java.lang.Thread.*;

public class JenkinsImporter {
    public static void main(String[] args) throws Exception {
        final Config config = Config.loadFromDisk();
        final BoneCPDataSource boneCp = config.createBoneCp();
        HTTPCache httpCache = HttpClient.createHttpCache(config);
        Abdera abdera = config.createAbdera();
        final JenkinsClient jenkinsClient = new JenkinsClient(httpCache, abdera);
        final ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(5);

        HashSet<JenkinsServerDto> servers = new HashSet<>();

        ObjectManager<JenkinsServerDto, ActorRef<JenkinsServerActor>> serverManager = new ObjectManager<>("JenkinsServerOld", servers, new ObjectFactory<JenkinsServerDto, ActorRef<JenkinsServerActor>>() {
            public ActorRef<JenkinsServerActor> create(JenkinsServerDto server) {
                String name = "Jenkins: " + server.url;
                return threadedActor(name, config.jenkinsUpdateInterval, boneCp, name, new JenkinsServerActor(jenkinsClient, server));
            }
        });

        final AtomicBoolean shouldRun = new AtomicBoolean(true);
        config.addShutdownHook(currentThread(), shouldRun);

        while (shouldRun.get()) {
            try {
                java.util.List<JenkinsServerDto> newKeys;

                try (Connection c = boneCp.getConnection()) {
                    newKeys = new JenkinsDao(c).selectServers(true);
                }

                serverManager.update(newKeys);
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }

//            for (ActorRef<JenkinsServer2> server : serverManager.getObjects()) {
//                Option<P2<JenkinsXml, LocalDateTime>> o = server.underlying().getJenkins();
//
//                if (o.isSome()) {
//                    P2<JenkinsXml, LocalDateTime> p = o.some();
//                    System.out.println("Last update: " + p._2() + ", jobs=" + p._1().jobs.size());
//                } else {
//                    System.out.println("Never updated: url=" + server.url);
//                }
//            }

            synchronized (shouldRun) {
                shouldRun.wait(1000);
            }
        }

        serverManager.close();
        executorService.shutdownNow();
    }
}

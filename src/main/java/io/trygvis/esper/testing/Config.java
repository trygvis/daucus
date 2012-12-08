package io.trygvis.esper.testing;

import ch.qos.logback.classic.*;
import ch.qos.logback.core.util.*;
import com.jolbox.bonecp.*;
import fj.data.*;
import static fj.data.Option.*;
import static org.apache.commons.lang.StringUtils.*;

import org.apache.abdera.*;
import org.slf4j.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class Config {
    public static class GitoriousConfig {
        public final String url;
        public final Option<String> sessionValue;

        public final long projectListUpdateDelay;

        public final long projectListUpdateInterval;

        public GitoriousConfig(String url, Option<String> sessionValue, long projectListUpdateDelay, long projectListUpdateInterval) {
            this.url = url;
            this.sessionValue = sessionValue;
            this.projectListUpdateDelay = projectListUpdateDelay;
            this.projectListUpdateInterval = projectListUpdateInterval;
        }

        public static GitoriousConfig fromProperties(Properties properties) {
            String key = "gitorious.sessionValue";
            return new GitoriousConfig(trimToNull(properties.getProperty("gitorious.url")),
                    getProperty(properties, key),
                    getProperty(properties, "gitorious.projectListUpdateDelay").bind(parseLong).valueE("Missing/bad value for 'gitorious.projectListUpdateDelay'"),
                    getProperty(properties, "gitorious.projectListUpdateInterval").bind(parseLong).valueE("Missing/bad value for 'gitorious.projectListUpdateInterval'"));
        }
    }

    public final GitoriousConfig gitorious;

    public final long nexusUpdateInterval;

    public final long jenkinsUpdateInterval;

    public final String databaseUrl;
    public final String databaseUsername;
    public final String databasePassword;

    public Config(GitoriousConfig gitorious, long nexusUpdateInterval, long jenkinsUpdateInterval, String databaseUrl,
                  String databaseUsername, String databasePassword) {
        this.gitorious = gitorious;
        this.nexusUpdateInterval = nexusUpdateInterval;
        this.jenkinsUpdateInterval = jenkinsUpdateInterval;
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
    }

    public static Config loadFromDisk() throws IOException {
        initLogging();

        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream("config.properties")) {
            properties.load(inputStream);
        }

        return new Config(GitoriousConfig.fromProperties(properties),
                getProperty(properties, "nexus.updateInterval").bind(parseInt).valueE("Missing required property: nexus.updateInterval") * 1000,
                getProperty(properties, "jenkins.updateInterval").bind(parseInt).valueE("Missing required property: jenkins.updateInterval") * 1000,
                trimToNull(properties.getProperty("database.url")),
                trimToNull(properties.getProperty("database.username")),
                trimToNull(properties.getProperty("database.password")));
    }

    private static void initLogging() {
        LoggerFactory.getILoggerFactory();
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//        StatusPrinter.print(lc);
    }

    public BoneCPDataSource createBoneCp() throws SQLException {
        return new BoneCPDataSource(new BoneCPConfig(){{
            setJdbcUrl(databaseUrl);
            setUsername(databaseUsername);
            setPassword(databasePassword);
            setDefaultAutoCommit(false);
            setCloseConnectionWatch(true);
            setMaxConnectionsPerPartition(10);
        }});
    }

    public Abdera createAbdera() {
        return new Abdera();
    }

    public void addShutdownHook(final Thread t, final AtomicBoolean shouldRun) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            {
                setName("Shutdown hook");
            }

            public void run() {
                synchronized (shouldRun) {
                    shouldRun.set(false);
                    shouldRun.notifyAll();
                    t.interrupt();
                }
            }
        });
    }

    private static Option<String> getProperty(Properties properties, String key) {
        return fromNull(trimToNull(properties.getProperty(key)));
    }
}

package io.trygvis.esper.testing;

import com.jolbox.bonecp.*;
import fj.data.*;
import org.apache.abdera.*;
import org.slf4j.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static fj.data.Option.*;
import static java.lang.System.err;
import static java.lang.System.exit;
import static org.apache.commons.lang.StringUtils.*;

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

    private BoneCPDataSource dataSource;

    public Config(GitoriousConfig gitorious, long nexusUpdateInterval, long jenkinsUpdateInterval, String databaseUrl,
                  String databaseUsername, String databasePassword) {
        this.gitorious = gitorious;
        this.nexusUpdateInterval = nexusUpdateInterval;
        this.jenkinsUpdateInterval = jenkinsUpdateInterval;
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
    }

    public static Config loadFromDisk(String appName) throws IOException {
        initSystemProperties();

        initLogging(appName);

        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream("etc/config.properties")) {
            properties.load(inputStream);
        }

        return new Config(GitoriousConfig.fromProperties(properties),
                getProperty(properties, "nexus.updateInterval").bind(parseInt).valueE("Missing required property: nexus.updateInterval") * 1000,
                getProperty(properties, "jenkins.updateInterval").bind(parseInt).valueE("Missing required property: jenkins.updateInterval") * 1000,
                trimToNull(properties.getProperty("database.url")),
                trimToNull(properties.getProperty("database.username")),
                trimToNull(properties.getProperty("database.password")));
    }

    private static void initSystemProperties() {
        // Java 7 is more strict on checking matching host names or something similar.
        // http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#Customization
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    private static void initLogging(String appName) {
        System.setProperty("logging.app", appName);

        File logs = new File("logs");

        if(!logs.isDirectory()) {
            if(!logs.mkdirs()) {
                err.println("Could not create logs directory: " + logs.getAbsolutePath());
                exit(1);
            }
        }

        LoggerFactory.getILoggerFactory();
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//        StatusPrinter.print(lc);
    }

    public BoneCPDataSource createBoneCp() throws SQLException {
        if (dataSource != null) {
            return dataSource;
        }

        return dataSource = new BoneCPDataSource(new BoneCPConfig() {{
            setJdbcUrl(databaseUrl);
            setUsername(databaseUsername);
            setPassword(databasePassword);
            setDefaultAutoCommit(false);
            setCloseConnectionWatch(false);
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

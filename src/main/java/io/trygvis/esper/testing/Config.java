package io.trygvis.esper.testing;

import ch.qos.logback.classic.*;
import ch.qos.logback.core.util.*;
import com.jolbox.bonecp.*;
import fj.data.*;
import static fj.data.Option.*;
import static org.apache.commons.lang.StringUtils.*;
import org.slf4j.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class Config {
    public final String gitoriousUrl;
    public final Option<String> gitoriousSessionValue;

    public final long nexusUpdateInterval;

    public final String databaseUrl;
    public final String databaseUsername;
    public final String databasePassword;

    public Config(String gitoriousUrl, Option<String> gitoriousSessionValue, long nexusUpdateInterval, String databaseUrl, String databaseUsername, String databasePassword) {
        this.gitoriousUrl = gitoriousUrl;
        this.gitoriousSessionValue = gitoriousSessionValue;
        this.nexusUpdateInterval = nexusUpdateInterval;
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

        return new Config(trimToNull(properties.getProperty("gitorious.url")),
                fromNull(trimToNull(properties.getProperty("gitorious.sessionValue"))),
                fromNull(trimToNull(properties.getProperty("nexus.updateInterval"))).bind(parseInt).some() * 1000,
                trimToNull(properties.getProperty("database.url")),
                trimToNull(properties.getProperty("database.username")),
                trimToNull(properties.getProperty("database.password")));
    }

    private static void initLogging() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
    }

    public BoneCPDataSource createBoneCp() throws SQLException {
        return new BoneCPDataSource(new BoneCPConfig(){{
            setJdbcUrl(databaseUrl);
            setUsername(databaseUsername);
            setPassword(databasePassword);
            setDefaultAutoCommit(false);
            setMaxConnectionsPerPartition(10);
        }});
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
}

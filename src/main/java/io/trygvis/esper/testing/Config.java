package io.trygvis.esper.testing;

import com.jolbox.bonecp.*;
import fj.data.*;
import org.apache.abdera.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.module.*;
import org.slf4j.*;
import org.slf4j.bridge.*;

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

    public final String appName;
    public final GitoriousConfig gitorious;

    public final long nexusUpdateInterval;

    public final long jenkinsUpdateInterval;

    public final String databaseUrl;
    public final String databaseUsername;
    public final String databasePassword;

    private BoneCPDataSource dataSource;

    public Config(String appName, GitoriousConfig gitorious,
                  long nexusUpdateInterval, long jenkinsUpdateInterval,
                  String databaseUrl, String databaseUsername, String databasePassword) {
        this.appName = appName;
        this.gitorious = gitorious;
        this.nexusUpdateInterval = nexusUpdateInterval;
        this.jenkinsUpdateInterval = jenkinsUpdateInterval;
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
    }

    public static Config loadFromDisk(String appName) throws IOException {
        initSystemProperties(appName);

        initLogging();

        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream("etc/config.properties")) {
            properties.load(inputStream);
        }

        return new Config(appName,
                GitoriousConfig.fromProperties(properties),
                getProperty(properties, "nexus.updateInterval").bind(parseInt).valueE("Missing required property: nexus.updateInterval") * 1000,
                getProperty(properties, "jenkins.updateInterval").bind(parseInt).valueE("Missing required property: jenkins.updateInterval") * 1000,
                trimToNull(properties.getProperty("database.url")),
                trimToNull(properties.getProperty("database.username")),
                trimToNull(properties.getProperty("database.password")));
    }

    private static void initSystemProperties(String appName) {
        // Java 7 is more strict on checking matching host names or something similar.
        // http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#Customization
        System.setProperty("jsse.enableSNIExtension", "false");

        System.setProperty("loggingApp", appName);
    }

    private static void initLogging() {
        File logs = new File("logs");

        if(!logs.isDirectory()) {
            if(!logs.mkdirs()) {
                err.println("Could not create logs directory: " + logs.getAbsolutePath());
                exit(1);
            }
        }

        LoggerFactory.getILoggerFactory();
//        ch.qos.logback.classic.LoggerContext lc = (ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory();
//        ch.qos.logback.core.util.StatusPrinter.print(lc);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
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

    public ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("wat", Version.unknownVersion());
        module.addDeserializer(Uuid.class, new UuidDeserializer());
        module.addSerializer(Uuid.class, new UuidSerializer());
        objectMapper.registerModule(module);
        return objectMapper;
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

    private static class UuidDeserializer extends JsonDeserializer<Uuid> {
        public Uuid deserialize(JsonParser jp, DeserializationContext context) throws IOException {
            return Uuid.fromString(jp.getText());
        }
    }

    private static class UuidSerializer extends JsonSerializer<Uuid> {
        public void serialize(Uuid value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(value.toStringBase64());
        }
    }
}

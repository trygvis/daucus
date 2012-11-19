package io.trygvis.esper.testing;

import ch.qos.logback.classic.*;
import ch.qos.logback.core.util.*;
import fj.data.*;
import java.io.*;
import java.util.*;
import org.apache.commons.httpclient.protocol.*;
import org.slf4j.*;

import static org.apache.commons.lang.StringUtils.*;

public class Config {
    public final String gitoriousUrl;
    public final Option<String> gitoriousSessionValue;
    public final String nexusUrl;

    public final String databaseDriver;
    public final String databaseUrl;
    public final String databaseUsername;
    public final String databasePassword;

    public Config(String gitoriousUrl, Option<String> gitoriousSessionValue, String nexusUrl, String databaseDriver, String databaseUrl, String databaseUsername, String databasePassword) {
        this.gitoriousUrl = gitoriousUrl;
        this.gitoriousSessionValue = gitoriousSessionValue;
        this.nexusUrl = nexusUrl;
        this.databaseDriver = databaseDriver;
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

        String driver = trimToNull(properties.getProperty("database.driver"));

//        Class.forName(driver);

        return new Config(trimToNull(properties.getProperty("gitorious.url")),
                Option.fromNull(trimToNull(properties.getProperty("gitorious.sessionValue"))),
                trimToNull(properties.getProperty("nexus.url")),
                driver,
                trimToNull(properties.getProperty("database.url")),
                trimToNull(properties.getProperty("database.username")),
                trimToNull(properties.getProperty("database.password")));
    }

    private static void initLogging() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
    }
}

package io.trygvis.esper.testing;

import fj.data.*;
import org.apache.commons.httpclient.protocol.*;
import static org.apache.commons.lang.StringUtils.*;
import org.apache.log4j.*;

import java.io.*;
import java.util.*;

public class Config {
    public final String gitoriousUrl;
    public final Option<String> gitoriousSessionValue;
    public final String nexusUrl;

    public Config(String gitoriousUrl, Option<String> gitoriousSessionValue, String nexusUrl) {
        this.gitoriousUrl = gitoriousUrl;
        this.gitoriousSessionValue = gitoriousSessionValue;
        this.nexusUrl = nexusUrl;
    }

    public static Config loadFromDisk() throws IOException {
        configureLog4j();

        Protocol.registerProtocol("https", new Protocol("https", (ProtocolSocketFactory) new EasySSLProtocolSocketFactory(), 443));

        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream("config.properties")) {
            properties.load(inputStream);
        }

        return new Config(trimToNull(properties.getProperty("gitorious.url")),
                Option.fromNull(trimToNull(properties.getProperty("gitorious.sessionValue"))),
                trimToNull(properties.getProperty("nexus.url")));
    }

    public static void configureLog4j() {
        Properties properties = new Properties();
        properties.setProperty("log4j.rootLogger", "DEBUG, A1");
        properties.setProperty("log4j.logger.httpclient.wire.content", "INFO");
        properties.setProperty("log4j.logger.httpclient.wire.header", "INFO");
        properties.setProperty("log4j.logger.org.apache.commons.httpclient", "INFO");
        properties.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        properties.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        properties.setProperty("log4j.appender.A1.layout.ConversionPattern", "%-4r [%t] %-5p %c %x - %m%n");
        PropertyConfigurator.configure(properties);
    }
}

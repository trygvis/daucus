package io.trygvis.esper.testing.web;

import io.trygvis.appsh.booter.jetty.*;
import org.slf4j.bridge.*;

import java.io.*;

public class WebRunner {

    public static void main(String[] args) throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        JettyWebServer server = new JettyWebServer();
        server.setHttpPort(1337);
        server.addContext("/", new File("src/main/webapp").getAbsoluteFile());
        server.run();
    }
}

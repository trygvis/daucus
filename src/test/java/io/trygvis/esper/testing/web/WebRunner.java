package io.trygvis.esper.testing.web;

import io.trygvis.appsh.booter.jetty.*;
import io.trygvis.esper.testing.*;

import java.io.*;

public class WebRunner {

    public static void main(String[] args) throws Exception {
        // Ensures that the system has been initialized
        @SuppressWarnings("UnusedDeclaration") Config config = WebConfig.config;

        JettyWebServer server = new JettyWebServer();
        server.setHttpPort(1337);
        File webapp = new File(WebRunner.class.getResource("/webapp").getPath());
        server.addContext(new JettyWebServer.WarContext("/", webapp));
        server.run();
    }
}

package io.trygvis.esper.testing.web;

import io.trygvis.esper.testing.*;

import java.io.*;

public class WebConfig {
    public static final Config config;

    static {
        try {
            config = Config.loadFromDisk("web");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

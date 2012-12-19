package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import org.joda.time.*;

import java.net.*;
import java.util.*;

public class JenkinsJobDto {
    public final UUID uuid;
    public final DateTime created_date;
    public final UUID server;
    public final URI url;
    public final Option<String> displayName;

    JenkinsJobDto(UUID uuid, DateTime created_date, UUID server, URI url, Option<String> displayName) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.server = server;
        this.url = url;
        this.displayName = displayName;
    }
}

package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.net.*;
import java.util.*;

public class JenkinsJobDto extends AbstractEntity {
    public final UUID server;
    public final URI url;
    public final Option<String> displayName;

    JenkinsJobDto(UUID uuid, DateTime createdDate, UUID server, URI url, Option<String> displayName) {
        super(uuid, createdDate);
        this.server = server;
        this.url = url;
        this.displayName = displayName;
    }
}

package io.trygvis.esper.testing.jenkins;

import fj.data.*;
import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.net.*;
import java.util.*;

public class JenkinsJobDto extends AbstractEntity {
    public final UUID server;
    public final UUID file;
    public final URI url;
    public final String jobType;
    public final Option<String> displayName;

    public JenkinsJobDto(UUID uuid, DateTime createdDate, UUID server, UUID file, URI url, String jobType, Option<String> displayName) {
        super(uuid, createdDate);
        this.server = server;
        this.file = file;
        this.url = url;
        this.jobType = jobType;
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName.orSome("<no name>");
    }
}

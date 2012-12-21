package io.trygvis.esper.testing.jenkins;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.net.*;
import java.util.*;

public class JenkinsBuildDto extends AbstractEntity {
    public final UUID job;
    public final String entryId;
    public final URI url;
    public final String result;
    public final int number;
    public final int duration;
    public final DateTime timestamp;
    public final UUID[] users;

    public JenkinsBuildDto(UUID uuid, DateTime createdDate, UUID job, String entryId, URI url, String result, int number, int duration, DateTime timestamp, UUID[] users) {
        super(uuid, createdDate);
        this.job = job;
        this.entryId = entryId;
        this.url = url;
        this.result = result;
        this.number = number;
        this.duration = duration;
        this.timestamp = timestamp;
        this.users = users;
    }
}

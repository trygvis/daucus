package io.trygvis.esper.testing.jenkins;

import org.joda.time.*;

import java.net.*;
import java.util.*;

public class JenkinsBuildDto {
    public final UUID uuid;
    public final DateTime created_date;
    public final UUID job;
    public final String entryId;
    public final URI url;
    public final String result;
    public final int number;
    public final int duration;
    public final DateTime timestamp;

    JenkinsBuildDto(UUID uuid, DateTime created_date, UUID job, String entryId, URI url, String result, int number, int duration, DateTime timestamp) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.job = job;
        this.entryId = entryId;
        this.url = url;
        this.result = result;
        this.number = number;
        this.duration = duration;
        this.timestamp = timestamp;
    }
}

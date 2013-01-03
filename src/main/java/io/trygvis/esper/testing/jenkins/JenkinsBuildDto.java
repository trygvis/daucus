package io.trygvis.esper.testing.jenkins;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.net.*;
import java.util.*;

import static io.trygvis.esper.testing.EntityRef.jenkinsBuildRef;

public class JenkinsBuildDto extends AbstractEntity {
    public final UUID job;
    public final UUID file;
    public final String entryId;
    public final URI url;
    public final UUID[] users;

    public JenkinsBuildDto(UUID uuid, DateTime createdDate, UUID job, UUID file, String entryId, URI url, UUID[] users) {
        super(uuid, createdDate);
        this.job = job;
        this.file = file;
        this.entryId = entryId;
        this.url = url;
        this.users = users;
    }

    public EntityRef toRef() {
        return jenkinsBuildRef(uuid);
    }
}

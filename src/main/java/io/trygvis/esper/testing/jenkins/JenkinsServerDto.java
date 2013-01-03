package io.trygvis.esper.testing.jenkins;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.net.*;
import java.util.*;

public class JenkinsServerDto extends AbstractEntity {
    public final URI url;
    public final boolean enabled;

    JenkinsServerDto(UUID uuid, DateTime createdDate, URI url, boolean enabled) {
        super(uuid, createdDate);
        this.url = url;
        this.enabled = enabled;
    }

    public URI userUrl(String id) {
        return URI.create(url.toASCIIString() + "/users/" + id);
    }
}

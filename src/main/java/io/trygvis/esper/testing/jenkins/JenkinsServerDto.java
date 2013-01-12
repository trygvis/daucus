package io.trygvis.esper.testing.jenkins;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.net.*;
import java.util.*;

public class JenkinsServerDto extends AbstractEntity {
    public final String name;
    public final URI url;
    public final boolean enabled;

    public JenkinsServerDto(UUID uuid, DateTime createdDate, String name, URI url, boolean enabled) {
        super(uuid, createdDate);
        this.name = name;
        this.url = url;
        this.enabled = enabled;
    }

    public URI userUrl(String id) {
        return URI.create(url.toASCIIString() + "/user/" + id);
    }
}

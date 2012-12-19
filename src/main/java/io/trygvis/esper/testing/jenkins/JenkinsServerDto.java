package io.trygvis.esper.testing.jenkins;

import org.joda.time.*;

import java.net.*;
import java.util.*;

public class JenkinsServerDto {
    public final UUID uuid;
    public final DateTime created_date;
    public final URI url;
    public final boolean enabled;

    JenkinsServerDto(UUID uuid, DateTime created_date, URI url, boolean enabled) {
        this.uuid = uuid;
        this.created_date = created_date;
        this.url = url;
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JenkinsServerDto that = (JenkinsServerDto) o;

        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}

package io.trygvis.esper.testing.jenkins;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.util.*;

public class JenkinsUserDto extends AbstractDto {
    public final UUID server;
    public final String absoluteUrl;

    public JenkinsUserDto(UUID uuid, DateTime createdDate, UUID server, String absoluteUrl) {
        super(uuid, createdDate);
        this.server = server;
        this.absoluteUrl = absoluteUrl;
    }
}

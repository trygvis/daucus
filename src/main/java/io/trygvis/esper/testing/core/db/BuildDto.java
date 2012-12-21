package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.util.*;

class BuildDto extends AbstractEntity {
    public final DateTime timestamp;
    public final boolean success;
    public final UUID referenceUuid;
    public final String referenceType;

    BuildDto(UUID uuid, DateTime createdDate, DateTime timestamp, boolean success, UUID referenceUuid, String referenceType) {
        super(uuid, createdDate);
        this.timestamp = timestamp;
        this.success = success;
        this.referenceUuid = referenceUuid;
        this.referenceType = referenceType;
    }
}

package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.util.*;

public class BuildDto extends AbstractEntity {
    public final DateTime timestamp;
    public final boolean success;
    public final EntityRef ref;

    public BuildDto(UUID uuid, DateTime createdDate, DateTime timestamp, boolean success, EntityRef ref) {
        super(uuid, createdDate);
        this.timestamp = timestamp;
        this.success = success;
        this.ref = ref;
    }
}

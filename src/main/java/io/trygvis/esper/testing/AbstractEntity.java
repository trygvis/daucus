package io.trygvis.esper.testing;

import org.joda.time.*;

import java.util.*;

public abstract class AbstractEntity {
    public final UUID uuid;
    public final DateTime createdDate;

    protected AbstractEntity(UUID uuid, DateTime createdDate) {
        this.uuid = uuid;
        this.createdDate = createdDate;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEntity)) return false;

        AbstractEntity that = (AbstractEntity) o;

        return uuid.equals(that.uuid);
    }

    public int hashCode() {
        return uuid.hashCode();
    }
}

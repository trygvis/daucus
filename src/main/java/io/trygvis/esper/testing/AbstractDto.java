package io.trygvis.esper.testing;

import org.joda.time.*;

import java.util.*;

public abstract class AbstractDto {
    public final UUID uuid;
    public final DateTime createdDate;

    protected AbstractDto(UUID uuid, DateTime createdDate) {
        this.uuid = uuid;
        this.createdDate = createdDate;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractDto)) return false;

        AbstractDto that = (AbstractDto) o;

        return uuid.equals(that.uuid);
    }

    public int hashCode() {
        return uuid.hashCode();
    }
}

package io.trygvis.esper.testing.core;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.util.*;

public class PersonDto extends AbstractEntity {
    public final String name;

    public PersonDto(UUID uuid, DateTime createdDate, String name) {
        super(uuid, createdDate);
        this.name = name;
    }
}

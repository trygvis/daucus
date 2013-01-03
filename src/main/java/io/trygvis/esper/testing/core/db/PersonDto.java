package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

import java.util.*;

public class PersonDto extends AbstractEntity {
    public final String name;
    public final String mail;

    public PersonDto(UUID uuid, DateTime createdDate, String name, String mail) {
        super(uuid, createdDate);
        this.name = name;
        this.mail = mail;
    }
}

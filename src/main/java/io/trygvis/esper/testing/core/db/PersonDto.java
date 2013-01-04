package io.trygvis.esper.testing.core.db;

import io.trygvis.esper.testing.*;
import org.joda.time.*;

public class PersonDto /*extends AbstractEntity*/ {
    public final Uuid uuid;
    public final DateTime createdDate;
    public final String name;
    public final String mail;

    public PersonDto(Uuid uuid, DateTime createdDate, String name, String mail) {
//        super(uuid, createdDate);
        this.uuid = uuid;
        this.createdDate = createdDate;
        this.name = name;
        this.mail = mail;
    }
}

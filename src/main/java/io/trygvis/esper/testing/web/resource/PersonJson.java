package io.trygvis.esper.testing.web.resource;

import io.trygvis.esper.testing.*;

import java.util.*;

import static io.trygvis.esper.testing.util.Gravatar.*;

public class PersonJson {
    public final Uuid uuid;
    public final String name;
    public final String mail;
    public final String gravatar;

    public PersonJson(Uuid uuid, String name, String mail) {
        this.uuid = uuid;
        this.name = name;
        this.mail = mail;

        gravatar = gravatar(mail);
    }
}

class PersonDetailJson {
    public final PersonJson person;
    public final List<BadgeJson> badges;
    public final List<BadgeJson> badgesInProgress;

    public PersonDetailJson(PersonJson person, List<BadgeJson> badges, List<BadgeJson> badgesInProgress) {
        this.person = person;
        this.badges = badges;
        this.badgesInProgress = badgesInProgress;
    }
}

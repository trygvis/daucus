package io.trygvis.esper.testing;

import java.util.*;

public abstract class EntityRef {
    public final UUID uuid;
    public final String type;

    protected EntityRef(UUID uuid, String type) {
        this.uuid = uuid;
        this.type = type;
    }

    public static class JenkinsRef extends EntityRef {
        private JenkinsRef(UUID uuid) {
            super(uuid, "jenkins");
        }
    }

    public static JenkinsRef jenkinsRef(UUID uuid) {
        return new JenkinsRef(uuid);
    }
}

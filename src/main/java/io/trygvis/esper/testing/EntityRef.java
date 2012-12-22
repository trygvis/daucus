package io.trygvis.esper.testing;

import java.sql.*;
import java.util.*;

public abstract class EntityRef {
    public final UUID uuid;
    public final String type;

    protected EntityRef(UUID uuid, String type) {
        this.uuid = uuid;
        this.type = type;
    }

    public static EntityRef fromRs(ResultSet rs, int i) throws SQLException {
        String type = rs.getString(i++);

        if (type == null) {
            throw new SQLException("reference type was null.");
        }

        UUID uuid = UUID.fromString(rs.getString(i));

        switch (type) {
            case "jenkins":
                return new JenkinsRef(uuid);
            default:
                throw new SQLException("Unknown reference type: " + type);
        }
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

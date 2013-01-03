package io.trygvis.esper.testing;

import java.sql.*;
import java.util.*;

import static io.trygvis.esper.testing.EntityRef.JenkinsBuildRef.*;

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
            case JENKINS_BUILD_REF:
                return new JenkinsBuildRef(uuid);
            default:
                throw new SQLException("Unknown reference type: " + type);
        }
    }

    public static class JenkinsBuildRef extends EntityRef {
        public static final String JENKINS_BUILD_REF = "jenkins-build";

        private JenkinsBuildRef(UUID uuid) {
            super(uuid, JENKINS_BUILD_REF);
        }
    }

    public static JenkinsBuildRef jenkinsBuildRef(UUID uuid) {
        return new JenkinsBuildRef(uuid);
    }
}

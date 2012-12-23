package io.trygvis.esper.testing.web;

import org.apache.shiro.realm.jdbc.*;

public class MissingShiroJdbcRealm extends JdbcRealm {
    public MissingShiroJdbcRealm() throws Exception {
        setDataSource(WebConfig.config.createBoneCp());
    }
}

package io.trygvis.esper.testing;

import com.jolbox.bonecp.*;
import io.trygvis.esper.testing.core.db.*;
import io.trygvis.esper.testing.jenkins.*;
import io.trygvis.esper.testing.util.sql.*;
import org.slf4j.*;

import java.sql.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

public class LdapImporter {
    public static void main(String[] args) throws Exception {
        Config config = Config.loadFromDisk("person-generator");

        final Logger logger = LoggerFactory.getLogger(config.appName);

        if(args.length != 3) {
            System.err.println("Usage: ldap-importer [ldap host] [ldap port] [base dn]");
            System.exit(1);
        }

        String ldapHost = args[0];
        int ldapPort = Integer.parseInt(args[1]);
        String baseDn = args[2];

        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        properties.put(Context.PROVIDER_URL, "ldap://" + ldapHost + ":" + ldapPort);
        properties.put(Context.REFERRAL, "ignore");
        properties.put(Context.SECURITY_AUTHENTICATION, "none");

        InitialDirContext context = new InitialDirContext(properties);
        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchCtls.setReturningAttributes(new String[]{"uid", "mail", "displayName"});
        NamingEnumeration answer = context.search(baseDn, "(&(uid=*)(mail=*)(displayName=*))", searchCtls);

        BoneCPDataSource dataSource = config.createBoneCp();
        final Daos daos = new Daos(dataSource.getConnection());
        PersonDao personDao = daos.personDao;
        List<JenkinsServerDto> servers = daos.jenkinsDao.selectServers(false);

        while (answer.hasMore()) {
            SearchResult next = (SearchResult) answer.next();
            String uid = next.getAttributes().get("uid").get().toString();
            String mail = next.getAttributes().get("mail").get().toString();
            String displayName = next.getAttributes().get("displayName").get().toString();

            logger.info("LDAP: uid = {}, mail = {}, displayName = {}", uid, mail, displayName);

            SqlOption<PersonDto> personO = personDao.selectPersonByMail(mail);

            Uuid personUuid;
            if(personO.isNone()) {
                personUuid = personDao.insertPerson(mail, displayName);
                logger.info("New person: uuid={}", personUuid);
            }
            else {
                personUuid = personO.get().uuid;
                logger.info("Existing person: uuid={}", personUuid);
            }

            claimUid(logger, daos, servers, uid, personUuid);

            // Add the username from their email addresses as aliases too
            int i = mail.indexOf('@');

            if (i == -1) {
                continue;
            }

            String alternate = mail.substring(0, i);
            
            if(alternate.equals(uid)) {
                continue;
            }
            
            claimUid(logger, daos, servers, alternate, personUuid);
        }

//        System.out.println("ROLLBACK");
//        daos.rollback();
        logger.info("COMMIT");
        daos.commit();
        logger.info("Closing SQL connection");
        daos.close();

        logger.info("Closing pool");
        dataSource.close();
        System.exit(0);
    }

    private static void claimUid(final Logger logger, final Daos daos, List<JenkinsServerDto> servers, String uid, Uuid personUuid) throws SQLException {
        for (final JenkinsServerDto server : servers) {
            final String url = server.userUrl(uid).toASCIIString();

            UUID jenkinsUserUuid = daos.jenkinsDao.selectUserByAbsoluteUrl(server.uuid, url).map(new SqlF<JenkinsUserDto, UUID>() {
                public UUID apply(JenkinsUserDto jenkinsUserDto) throws SQLException {
                    return jenkinsUserDto.uuid;
                }
            }).getOrElse(new SqlP0<UUID>() {
                public UUID apply() throws SQLException {
                    logger.info("New Jenkins user for server {}", server.url);
                    return daos.jenkinsDao.insertUser(server.uuid, url);
                }
            });

            if(!daos.personDao.hasPersonJenkinsUser(personUuid, jenkinsUserUuid)) {
                logger.info("Person claims jenkins user: " + url);
                daos.personDao.insertPersonJenkinsUser(personUuid, jenkinsUserUuid);
            }
            else {
                logger.info("Person already had a claim for: " + url);
            }
        }
    }
}

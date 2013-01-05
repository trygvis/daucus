BEGIN;

INSERT INTO nexus_server (uuid, url, name) VALUES ('4666dba4-3e2e-11e2-8a1b-0bd430e00b36', 'https://oss.sonatype.org', 'Central Repository');
INSERT INTO nexus_repository (uuid, server, id, group_ids) VALUES ('4a2d7ab2-3e2f-11e2-af03-eb1ace2381bb', '4666dba4-3e2e-11e2-8a1b-0bd430e00b36', 'releases', ARRAY ['io.trygvis', 'no.arktekk', 'org.codehaus']);

INSERT INTO nexus_server (uuid, url, name) VALUES ('91d942d8-3e2f-11e2-aaa0-a70628365abd', 'http://nexus.codehaus.org', 'Codehaus Snapshots');
INSERT INTO nexus_repository (uuid, server, id, group_ids) VALUES ('a2415b88-3e2f-11e2-a2b8-2f066b90cf13', '91d942d8-3e2f-11e2-aaa0-a70628365abd', 'snapshots', ARRAY ['org.codehaus.mojo']);

COMMIT;

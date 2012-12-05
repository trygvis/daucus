BEGIN;

DROP TABLE IF EXISTS nexus_event;
DROP TABLE IF EXISTS nexus_artifact;
DROP TABLE IF EXISTS nexus_repository;
DROP TABLE IF EXISTS nexus_server;

CREATE TABLE nexus_server (
  uuid CHAR(36)      NOT NULL,
  url  VARCHAR(1000) NOT NULL,
  name VARCHAR(1000) NOT NULL,
  CONSTRAINT pk_nexus_server PRIMARY KEY (uuid),
  CONSTRAINT uq_nexus_server_url UNIQUE (url)
);

CREATE TABLE nexus_repository (
  uuid      CHAR(36) NOT NULL,
  server    CHAR(36) NOT NULL,
  id        VARCHAR(100),
  group_ids VARCHAR(100) [],
  CONSTRAINT pk_nexus_repository PRIMARY KEY (uuid),
  CONSTRAINT fk_nexus_server FOREIGN KEY (server) REFERENCES nexus_server (uuid),
  CONSTRAINT uq_nexus_repository_id UNIQUE (server, id)
);

CREATE TABLE nexus_artifact (
  uuid        CHAR(36)     NOT NULL,
  repository  CHAR(36)     NOT NULL,
  group_id    VARCHAR(100) NOT NULL,
  artifact_id VARCHAR(100) NOT NULL,
  version     VARCHAR(100) NOT NULL,
  CONSTRAINT pk_nexus_artifact PRIMARY KEY (uuid),
  CONSTRAINT uq_nexus_artifact_gid_aid_version UNIQUE (group_id, artifact_id, version),
  CONSTRAINT fk_nexus_repository FOREIGN KEY (repository) REFERENCES nexus_repository (uuid)
);

CREATE TABLE nexus_event (
  uuid               CHAR(36)     NOT NULL,
  artifact           CHAR(36)     NOT NULL,
  timestamp          VARCHAR(100),
  guid               VARCHAR(1000),

  type               VARCHAR(100) NOT NULL,

-- new snapshot event
  snapshot_timestamp VARCHAR(1000),
  file               VARCHAR(1000),
  CONSTRAINT pk_nexus_event PRIMARY KEY (uuid),
  CONSTRAINT fk_nexus_artifact FOREIGN KEY (artifact) REFERENCES nexus_artifact (uuid),
  CONSTRAINT uq_guid UNIQUE (guid),
  CONSTRAINT check_event_type CHECK (type IN ('new_snapshot'))
--   CONSTRAINT pk_nexus_event PRIMARY KEY (timestamp, server_url, repository_id, group_id, artifact_id, version)
);

INSERT INTO nexus_server (uuid, url, name) VALUES ('4666dba4-3e2e-11e2-8a1b-0bd430e00b36', 'https://oss.sonatype.org', 'Central Repository');
INSERT INTO nexus_repository (uuid, server, id, group_ids) VALUES ('4a2d7ab2-3e2f-11e2-af03-eb1ace2381bb', '4666dba4-3e2e-11e2-8a1b-0bd430e00b36', 'releases', ARRAY ['io.trygvis', 'no.arktekk', 'org.codehaus']);

INSERT INTO nexus_server (uuid, url, name) VALUES ('91d942d8-3e2f-11e2-aaa0-a70628365abd', 'http://nexus.codehaus.org', 'Codehaus Snapshots');
INSERT INTO nexus_repository (uuid, server, id, group_ids) VALUES ('a2415b88-3e2f-11e2-a2b8-2f066b90cf13', '91d942d8-3e2f-11e2-aaa0-a70628365abd', 'snapshots', ARRAY ['org.codehaus.mojo']);

COMMIT;

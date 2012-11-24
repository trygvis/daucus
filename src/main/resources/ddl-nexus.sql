BEGIN;

DROP TABLE IF EXISTS nexus_event;
DROP TABLE IF EXISTS nexus_artifact;
DROP TABLE IF EXISTS nexus_repository;
DROP TABLE IF EXISTS nexus_server;

CREATE TABLE nexus_server (
  url  VARCHAR(1000) PRIMARY KEY,
  name VARCHAR(100) NOT NULL
);

CREATE TABLE nexus_repository (
  id                     VARCHAR(1000),
  server_url             VARCHAR(1000) NOT NULL,
  group_ids              VARCHAR(100) [],
  created_date           TIMESTAMP,
  last_update            TIMESTAMP,
  last_successful_update TIMESTAMP,
  CONSTRAINT nexus_repository_pk PRIMARY KEY (id, server_url),
  CONSTRAINT nexus_server_fk FOREIGN KEY (server_url) REFERENCES nexus_server (url)
);

CREATE TABLE nexus_artifact (
  server_url             VARCHAR(1000) NOT NULL,
  repository_id          VARCHAR(1000) NOT NULL,
  group_id               VARCHAR(100)  NOT NULL,
  artifact_id            VARCHAR(100)  NOT NULL,
  version                VARCHAR(100)  NOT NULL,
  snapshot_version       VARCHAR(100),
  classifiers            VARCHAR(100) [],
  packagings             VARCHAR(100) [],
  created_date           TIMESTAMP,
  last_update            TIMESTAMP,
  last_successful_update TIMESTAMP,
  CONSTRAINT nexus_artifact_pk PRIMARY KEY (server_url, repository_id, group_id, artifact_id, version),
  CONSTRAINT nexus_repository_fk FOREIGN KEY (server_url, repository_id) REFERENCES nexus_repository (server_url, id)
);

CREATE TABLE nexus_event (
  timestamp     VARCHAR(100),
  uuid          VARCHAR(100),
  server_url    VARCHAR(1000) NOT NULL,
  repository_id VARCHAR(1000) NOT NULL,
  group_id      VARCHAR(100)  NOT NULL,
  artifact_id   VARCHAR(100)  NOT NULL,
  version       VARCHAR(100)  NOT NULL,
  files         VARCHAR(100) [],
  CONSTRAINT nexus_event_pk PRIMARY KEY (timestamp, server_url, repository_id, group_id, artifact_id, version),
  CONSTRAINT nexus_artifact_fk FOREIGN KEY (server_url, repository_id, group_id, artifact_id, version) REFERENCES nexus_artifact (server_url, repository_id, group_id, artifact_id, version)
);

INSERT INTO nexus_server (url, name) VALUES ('https://oss.sonatype.org', 'Central Repository');
-- INSERT INTO nexus_repository (id, server_url, group_ids) VALUES ('releases', 'https://oss.sonatype.org', ARRAY ['io.trygvis', 'no.arktekk', 'org.codehaus']);
INSERT INTO nexus_repository (id, server_url, group_ids) VALUES ('releases', 'https://oss.sonatype.org', ARRAY ['io.trygvis', 'no.arktekk', 'org.codehaus']);

INSERT INTO nexus_server (url, name) VALUES ('http://nexus.codehaus.org', 'Codehaus Snapshots');
INSERT INTO nexus_repository (id, server_url, group_ids) VALUES ('snapshots', 'http://nexus.codehaus.org', ARRAY ['org.codehaus.mojo']);

COMMIT;

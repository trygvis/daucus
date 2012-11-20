BEGIN;

DROP TABLE IF EXISTS nexus_event;
DROP TABLE IF EXISTS nexus_repository;
DROP TABLE IF EXISTS nexus_server;

CREATE TABLE nexus_server (
  url VARCHAR(1000) PRIMARY KEY
);

CREATE TABLE nexus_repository (
  id                     VARCHAR(1000) PRIMARY KEY,
  nexus_server_url       VARCHAR(1000),
  group_ids              VARCHAR(100) [],
  created_date           TIMESTAMP,
  last_update            TIMESTAMP,
  last_successful_update TIMESTAMP,
  CONSTRAINT nexus_server_fk FOREIGN KEY (nexus_server_url) REFERENCES nexus_server (url)
);

CREATE TABLE nexus_event (
  repository_id VARCHAR(1000) NOT NULL,

  groupId       VARCHAR(100),
  artifactId    VARCHAR(100),
  version       VARCHAR(100),
  files         VARCHAR(100),
  CONSTRAINT nexus_repository_fk FOREIGN KEY (repository_id) REFERENCES nexus_repository (id)
);

INSERT INTO nexus_server(url) VALUES('https://oss.sonatype.org');
INSERT INTO nexus_repository(id, nexus_server_url, group_ids) VALUES('codehaus', 'https://oss.sonatype.org', ARRAY['io.trygvis', 'org.codehaus.mojo']);

COMMIT;

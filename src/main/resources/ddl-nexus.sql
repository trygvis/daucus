BEGIN;

DROP TABLE IF EXISTS nexus_event;
DROP TABLE IF EXISTS nexus_repository;

CREATE TABLE nexus_repository (
  id                     VARCHAR(1000) PRIMARY KEY,
  discovered_date        TIMESTAMP,
  last_update            TIMESTAMP,
  last_successful_update TIMESTAMP
);

CREATE TABLE nexus_event (
  repository_id VARCHAR(1000) NOT NULL,

  groupId       VARCHAR(100),
  artifactId    VARCHAR(100),
  version       VARCHAR(100),
  files         VARCHAR(100),
  CONSTRAINT nexus_repository_fk FOREIGN KEY (repository_id) REFERENCES nexus_repository (id)
);

COMMIT;

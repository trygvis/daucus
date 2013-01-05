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
  CONSTRAINT fk_nexus_repository__nexus_server FOREIGN KEY (server) REFERENCES nexus_server (uuid),
  CONSTRAINT uq_nexus_repository__id UNIQUE (server, id)
);

CREATE TABLE nexus_artifact (
  uuid        CHAR(36)     NOT NULL,
  repository  CHAR(36)     NOT NULL,
  group_id    VARCHAR(100) NOT NULL,
  artifact_id VARCHAR(100) NOT NULL,
  version     VARCHAR(100) NOT NULL,
  CONSTRAINT pk_nexus_artifact PRIMARY KEY (uuid),
  CONSTRAINT uq_nexus_artifact__gid__aid__version UNIQUE (group_id, artifact_id, version),
  CONSTRAINT fk_nexus_artifact__nexus_repository FOREIGN KEY (repository) REFERENCES nexus_repository (uuid)
);

CREATE TABLE nexus_event (
  uuid               CHAR(36)      NOT NULL,
  artifact           CHAR(36)      NOT NULL,
  created            TIMESTAMP     NOT NULL,

-- From the RSS
  guid               VARCHAR(1000) NOT NULL,
  date               TIMESTAMP     NOT NULL,

-- Our type flag
  type               VARCHAR(100)  NOT NULL,

-- new snapshot event
  snapshot_timestamp VARCHAR(100),
  build_number       INT,
  file               VARCHAR(1000),
  who                VARCHAR(1000),
  CONSTRAINT pk_nexus_event PRIMARY KEY (uuid),
  CONSTRAINT fk_nexus_event__artifact FOREIGN KEY (artifact) REFERENCES nexus_artifact (uuid),
  CONSTRAINT uq_nexus_event__guid UNIQUE (guid),
  CONSTRAINT check_event_type CHECK (type IN ('new_snapshot', 'new_release'))
--   CONSTRAINT pk_nexus_event PRIMARY KEY (timestamp, server_url, repository_id, group_id, artifact_id, version)
);

COMMIT;

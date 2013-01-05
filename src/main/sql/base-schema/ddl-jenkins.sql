BEGIN;

DROP TABLE IF EXISTS jenkins_build;
DROP TABLE IF EXISTS jenkins_job;
DROP TABLE IF EXISTS jenkins_server;

CREATE TABLE jenkins_server (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,

  url          VARCHAR(1000) NOT NULL,
  enabled      BOOLEAN       NOT NULL,

  CONSTRAINT pk_jenkins_server PRIMARY KEY (uuid),
  CONSTRAINT uq_jenkins_server__url UNIQUE (url)
);

CREATE TABLE jenkins_job (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,

  server       CHAR(36)      NOT NULL,

  file         CHAR(36)      NOT NULL,
  url          VARCHAR(1000) NOT NULL,
  job_type     VARCHAR(100)  NOT NULL,
  display_name VARCHAR(100),

  CONSTRAINT pk_jenkins_job PRIMARY KEY (UUID),
  CONSTRAINT fk_jenkins_job__server FOREIGN KEY (server) REFERENCES jenkins_server (uuid),
  CONSTRAINT fk_jenkins_job__file FOREIGN KEY (file) REFERENCES file (uuid),
  CONSTRAINT uq_jenkins_job__url UNIQUE (url)
);

CREATE SEQUENCE jenkins_build_seq;

CREATE TABLE jenkins_build (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,
  seq          INT           NOT NULL DEFAULT nextval('jenkins_build_seq'),

  job          CHAR(36)      NOT NULL,

  file         CHAR(36)      NOT NULL,
  entry_id     VARCHAR(1000) NOT NULL,
  url          VARCHAR(1000) NOT NULL,
  result       VARCHAR(100)  NOT NULL,
  number       INT           NOT NULL,
  duration     INT           NOT NULL,
  timestamp    TIMESTAMP     NOT NULL,
  users        CHAR(36) [],

  CONSTRAINT pk_jenkins_build PRIMARY KEY (UUID),
  CONSTRAINT fk_jenkins_build__job FOREIGN KEY (job) REFERENCES jenkins_job (uuid),
  CONSTRAINT fk_jenkins_build__file FOREIGN KEY (file) REFERENCES file (uuid),
  CONSTRAINT uq_jenkins_build__id UNIQUE (entry_id),
  CONSTRAINT uq_jenkins_build__seq UNIQUE (seq)
);

CREATE INDEX ix_jenkins_build__created_date ON jenkins_build (created_date);

CREATE TABLE jenkins_user (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,

  server       CHAR(36)      NOT NULL,
  absolute_url VARCHAR(1000) NOT NULL,
  CONSTRAINT pk_jenkins_user PRIMARY KEY (uuid),
  CONSTRAINT fk_jenkins_user__server FOREIGN KEY (server) REFERENCES jenkins_server (uuid),
  CONSTRAINT uq_jenkins_user__absolute_url UNIQUE (absolute_url)
);

COMMIT;

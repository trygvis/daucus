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

  url          VARCHAR(1000) NOT NULL,
  job_type     VARCHAR(100)  NOT NULL,
  display_name VARCHAR(100),

  CONSTRAINT pk_jenkins_job PRIMARY KEY (UUID),
  CONSTRAINT fk_jenkins_job__server FOREIGN KEY (server) REFERENCES jenkins_server (uuid),
  CONSTRAINT uq_jenkins_job__url UNIQUE (url)
);

CREATE SEQUENCE jenkins_build_seq;

CREATE TABLE jenkins_build (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,
  seq          INT           NOT NULL DEFAULT nextval('jenkins_build_seq'),

  job          CHAR(36)      NOT NULL,

  entry_id     VARCHAR(1000) NOT NULL,
  url          VARCHAR(1000) NOT NULL,
  result       VARCHAR(100)  NOT NULL,
  number       INT           NOT NULL,
  duration     INT           NOT NULL,
  timestamp    TIMESTAMP     NOT NULL,
  users        CHAR(36) [],

  CONSTRAINT pk_jenkins_build PRIMARY KEY (UUID),
  CONSTRAINT fk_jenkins_build__job FOREIGN KEY (job) REFERENCES jenkins_job (uuid),
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

INSERT INTO jenkins_server (uuid, created_date, url, enabled) VALUES
('782a75f6-40a4-11e2-aca6-20cf30557fa0', CURRENT_TIMESTAMP, 'https://builds.apache.org', FALSE),
('4c473c86-40ad-11e2-ae61-20cf30557fa0', CURRENT_TIMESTAMP, 'http://ci.jruby.org', FALSE),
('518c6162-411b-11e2-b63c-20cf30557fa0', CURRENT_TIMESTAMP, 'http://www.simantics.org/jenkins', FALSE),
('3c1a1448-422c-11e2-a7b3-20cf30557fa0', CURRENT_TIMESTAMP, 'https://jenkins.puppetlabs.com', FALSE);

COMMIT;

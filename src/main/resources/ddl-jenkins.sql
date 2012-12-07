BEGIN;

DROP TABLE IF EXISTS jenkins_build;
DROP TABLE IF EXISTS jenkins_server;

CREATE TABLE jenkins_server (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,
  url          VARCHAR(1000) NOT NULL,
  CONSTRAINT pk_jenkins_server PRIMARY KEY (uuid),
  CONSTRAINT uq_jenkins_server__url UNIQUE (url)
);

CREATE TABLE jenkins_build (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,

  entry_id     VARCHAR(1000) NOT NULL,
  url          VARCHAR(1000) NOT NULL,
  result       VARCHAR(100)  NOT NULL,
  number       INT           NOT NULL,
  duration     INT           NOT NULL,
  timestamp    TIMESTAMP     NOT NULL,

  CONSTRAINT pk_jenkins_build PRIMARY KEY (UUID),
  CONSTRAINT uq_jenkins_build__id UNIQUE (entry_id)
);

-- INSERT INTO jenkins_server (uuid, url) VALUES ('782a75f6-40a4-11e2-aca6-20cf30557fa0', 'https://builds.apache.org');
INSERT INTO jenkins_server (uuid, created_date, url) VALUES ('4c473c86-40ad-11e2-ae61-20cf30557fa0', current_timestamp, 'http://ci.jruby.org');

COMMIT;

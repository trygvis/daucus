BEGIN;

DROP TABLE IF EXISTS jenkins_server;

CREATE TABLE jenkins_server (
  uuid CHAR(36)      NOT NULL,
  url  VARCHAR(1000) NOT NULL,
  CONSTRAINT pk_jenkins_server PRIMARY KEY (uuid),
  CONSTRAINT uq_jenkins_server__url UNIQUE (url)
);

-- INSERT INTO jenkins_server (uuid, url) VALUES ('782a75f6-40a4-11e2-aca6-20cf30557fa0', 'https://builds.apache.org');
INSERT INTO jenkins_server (uuid, url) VALUES ('4c473c86-40ad-11e2-ae61-20cf30557fa0', 'http://ci.jruby.org');

COMMIT;

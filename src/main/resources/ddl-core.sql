BEGIN;

DROP TABLE IF EXISTS build_participant;
DROP TABLE IF EXISTS build;
DROP SEQUENCE IF EXISTS build_seq;
DROP TABLE IF EXISTS person_badge_progress;
DROP TABLE IF EXISTS person_badge;
DROP TABLE IF EXISTS person_jenkins_user;
DROP TABLE IF EXISTS person;
DROP TABLE IF EXISTS table_poller_status;
DROP TABLE IF EXISTS file;

CREATE TABLE file (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,
  url          VARCHAR(1000) NOT NULL,
  content_type VARCHAR(100)  NOT NULL,
  data         BYTEA,
  CONSTRAINT pk_file PRIMARY KEY (uuid)
);

CREATE TABLE table_poller_status (
  poller_name VARCHAR(100) NOT NULL,
  last_seq    INT          NOT NULL,
  last_run    TIMESTAMP,
  duration    INT,
  status      VARCHAR(1000),
  CONSTRAINT pk_job_status PRIMARY KEY (poller_name)
);

CREATE TABLE person (
  uuid         CHAR(36)  NOT NULL,
  created_date TIMESTAMP NOT NULL,
  name         VARCHAR(100),
  CONSTRAINT pk_person PRIMARY KEY (uuid)
);

--The users from the different jenkins servers this user has claimed
CREATE TABLE person_jenkins_user (
  person       CHAR(36),
  jenkins_user CHAR(36),
  CONSTRAINT pk_person_jenkins_user PRIMARY KEY (person, jenkins_user),
  CONSTRAINT fk_person_jenkins_user__person FOREIGN KEY (person) REFERENCES person (uuid),
  CONSTRAINT fk_person_jenkins_user__jenkins_user FOREIGN KEY (jenkins_user) REFERENCES jenkins_user (uuid)
);

-- TODO: create a table with all badges?

-- Badges received
CREATE TABLE person_badge (
  uuid         CHAR(36)     NOT NULL,
  created_date TIMESTAMP    NOT NULL,

  person       CHAR(36)     NOT NULL,
  name         VARCHAR(100) NOT NULL,
  level        INT          NOT NULL,
  count        INT          NOT NULL,

  CONSTRAINT pk_person_badge PRIMARY KEY (uuid),
  CONSTRAINT uq_person_badge__person__name__level UNIQUE (person, name, level),
  CONSTRAINT fk_person_badge__person FOREIGN KEY (person) REFERENCES person (uuid)
);

-- Badges the person is working on
CREATE TABLE person_badge_progress (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,

  person       CHAR(36)      NOT NULL,
  badge        VARCHAR(100)  NOT NULL,
  state        VARCHAR(8000) NOT NULL,

  CONSTRAINT pk_person_badge_progress PRIMARY KEY (uuid),
  CONSTRAINT fk_person_badge_progress__person FOREIGN KEY (person) REFERENCES person (uuid),
  CONSTRAINT uq_person_badge_progress__person_badge UNIQUE (person, badge)
);

CREATE SEQUENCE build_seq;

CREATE TABLE build (
  uuid           CHAR(36)     NOT NULL,
  created_date   TIMESTAMP    NOT NULL,

  seq            INT          NOT NULL DEFAULT nextval('build_seq'),
  timestamp      TIMESTAMP    NOT NULL,
  success        BOOL         NOT NULL,

  reference_type VARCHAR(100) NOT NULL,
  reference_uuid CHAR(36)     NOT NULL,

  CONSTRAINT pk_build PRIMARY KEY (uuid)
);

CREATE TABLE build_participant (
  build  CHAR(36) NOT NULL,
  person CHAR(36) NOT NULL,
  CONSTRAINT pk_build_participant PRIMARY KEY (build, person),
  CONSTRAINT fk_build_participant__build FOREIGN KEY (build) REFERENCES build (uuid),
  CONSTRAINT fk_build_participant__person FOREIGN KEY (person) REFERENCES person (uuid)
);

COMMIT;

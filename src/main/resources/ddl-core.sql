BEGIN;

CREATE TABLE table_poller_status (
  poller_name       VARCHAR(100) NOT NULL,
  last_created_date TIMESTAMP,
  last_run          TIMESTAMP,
  duration          INT,
  status            VARCHAR(1000),
  CONSTRAINT pk_job_status PRIMARY KEY (poller_name)
);

CREATE TABLE person (
  uuid          CHAR(36)  NOT NULL,
  created_date  TIMESTAMP NOT NULL,
--The users from the different jenkins servers this user has claimed
  jenkins_users CHAR(36) [],
  CONSTRAINT pk_person PRIMARY KEY (uuid)
);

-- Badges received
CREATE TABLE person_badge (
  uuid         CHAR(36)  NOT NULL,
  created_date TIMESTAMP NOT NULL,
  CONSTRAINT pk_person_badge PRIMARY KEY (uuid)
);

-- Badges the person is working on
CREATE TABLE person_badge_progress (
  uuid         CHAR(36)     NOT NULL,
  created_date TIMESTAMP    NOT NULL,

  name         VARCHAR(100) NOT NULL,

  CONSTRAINT pk_person_badge_progress PRIMARY KEY (uuid)
);

COMMIT;

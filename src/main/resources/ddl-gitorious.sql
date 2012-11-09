BEGIN;

DROP TABLE IF EXISTS gitorious_event;
DROP TABLE IF EXISTS gitorious_repository;
DROP TABLE IF EXISTS gitorious_project;

CREATE TABLE gitorious_project (
  slug VARCHAR(1000) PRIMARY KEY
);

CREATE TABLE gitorious_repository (
  project_slug           VARCHAR(1000) NOT NULL,
  name                   VARCHAR(1000) NOT NULL,
  atom_feed              VARCHAR(1000) NOT NULL,
  last_update            TIMESTAMP,
  last_successful_update TIMESTAMP,
  CONSTRAINT gitorious_repository_pk PRIMARY KEY (project_slug, name),
  CONSTRAINT gitorious_repository_2_gitorious_project FOREIGN KEY (project_slug) REFERENCES gitorious_project (slug)
);

CREATE TABLE gitorious_event (
  project_slug VARCHAR(1000) NOT NULL,
  name         VARCHAR(1000) NOT NULL,

-- The raw values for debugging
  entry_id     VARCHAR(1000) PRIMARY KEY,
  published    TIMESTAMP     NOT NULL,
  title        VARCHAR(1000),
  content      VARCHAR(1000),

  event_type   VARCHAR(20),
  who          VARCHAR(100),
-- Push
  "from"       CHAR(40),
  "to"         CHAR(40),
  branch       VARCHAR(100),
  commit_count INTEGER
);

INSERT INTO gitorious_project VALUES ('esper-test-project');
INSERT INTO gitorious_repository VALUES ('esper-test-project', 'esper-test-project', 'https://gitorious.org/esper-test-project/esper-test-project.atom');

COMMIT;

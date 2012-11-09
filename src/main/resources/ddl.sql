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
    entry_id VARCHAR(1000) PRIMARY KEY,
    text     VARCHAR(1000)
);

COMMIT;

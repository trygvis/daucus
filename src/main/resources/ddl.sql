BEGIN;

DROP TABLE IF EXISTS gitorious_change;
DROP TABLE IF EXISTS gitorious_repository;
DROP TABLE IF EXISTS gitorious_project;
DROP TABLE IF EXISTS atom_feed;

CREATE TABLE atom_feed (
  url VARCHAR(1000) PRIMARY KEY,
  last_update TIMESTAMP NOT NULL
);

CREATE TABLE gitorious_project (
  slug VARCHAR(1000) PRIMARY KEY
);

CREATE TABLE gitorious_repository (
  project_slug VARCHAR(1000) NOT NULL,
  name VARCHAR(1000) NOT NULL,
  atom_feed VARCHAR(1000) NOT NULL,
  CONSTRAINT gitorious_repository_pk PRIMARY KEY(project_slug, name),
  CONSTRAINT gitorious_repository_2_gitorious_project FOREIGN KEY(project_slug) REFERENCES gitorious_project(slug)
);

CREATE TABLE gitorious_change (
  entry_id VARCHAR(1000) PRIMARY KEY,
  text VARCHAR(1000)
);

COMMIT;

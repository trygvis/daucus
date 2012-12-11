BEGIN;

DROP TABLE IF EXISTS subscription_gitorious_repository;
DROP TABLE IF EXISTS subscriber;

CREATE TABLE subscriber (
  name VARCHAR(100) PRIMARY KEY
);

CREATE TABLE subscription_gitorious_repository (
  subscriber_name                   VARCHAR(100) REFERENCES subscriber (name),
  gitorious_repository_project_slug VARCHAR(100),
  gitorious_repository_name         VARCHAR(100),
  CONSTRAINT gitorious_repository FOREIGN KEY (gitorious_repository_project_slug, gitorious_repository_name) REFERENCES gitorious_repository (project_slug, name)
);

COMMIT;

BEGIN;

DROP TABLE IF EXISTS gitorious_change;
DROP TABLE IF EXISTS atom_feed;

CREATE TABLE atom_feed (
  url VARCHAR(1000) PRIMARY KEY,
  last_update TIMESTAMP NOT NULL
);

CREATE TABLE gitorious_repository (
  entry_id VARCHAR(1000) PRIMARY KEY,
  text VARCHAR(1000)
);

CREATE TABLE gitorious_change (
  entry_id VARCHAR(1000) PRIMARY KEY,
  text VARCHAR(1000)
);

COMMIT;

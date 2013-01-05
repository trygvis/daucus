BEGIN;

DROP TABLE IF EXISTS file;

CREATE TABLE file (
  uuid         CHAR(36)      NOT NULL,
  created_date TIMESTAMP     NOT NULL,
  url          VARCHAR(1000) NOT NULL,
  content_type VARCHAR(100)  NOT NULL,
  data         BYTEA,
  CONSTRAINT pk_file PRIMARY KEY (uuid)
);

COMMIT;

ALTER TABLE person_badge ADD state VARCHAR(8000);

ALTER TABLE person_badge DROP CONSTRAINT uq_person_badge__person__name__level;

ALTER TABLE person_badge ALTER count DROP NOT NULL;

DO $$
DECLARE r RECORD;
DECLARE zz RECORD;
BEGIN
  FOR r IN SELECT uuid, created_date, person, name, level, count FROM person_badge
  LOOP
    FOR zz IN SELECT x FROM generate_series(1, r.count) AS x
    LOOP
      INSERT INTO person_badge(uuid, created_date, person, name, level, state) VALUES(
        uuid_generate_v1(), r.created_date, r.person, r.name, r.level,
        '{"type":"' || r.name || '","person":"' || r.person || '","builds":[]}');
    END LOOP;
  END LOOP;
END$$;

ALTER TABLE person_badge DROP count;

DELETE FROM person_badge WHERE state IS NULL;

ALTER TABLE person_badge ALTER state SET NOT NULL;

--//@UNDO

ALTER TABLE person_badge ADD count INT;

ALTER TABLE person_badge ALTER state DROP NOT NULL;

INSERT into person_badge(uuid, created_date, name, person, level, count)
  SELECT uuid_generate_v1(), min(created_date) as "created_date", name, person, level, count(level) from person_badge group by name, person, level;

DELETE FROM person_badge WHERE count IS NULL;

ALTER TABLE person_badge ALTER count SET NOT NULL;

ALTER TABLE person_badge DROP state;

ALTER TABLE person_badge ADD CONSTRAINT uq_person_badge__person__name__level UNIQUE (person, name, level);

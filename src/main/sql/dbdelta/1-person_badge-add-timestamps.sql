ALTER TABLE person_badge ADD state VARCHAR(8000);

ALTER TABLE person_badge DROP CONSTRAINT uq_person_badge__person__name__level;

DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN SELECT uuid, created_date, person, count FROM person_badge
  LOOP
    FOR x IN SELECT x FROM generate_series(1, r.count)
    LOOP
      INSERT INTO person_badge(uuid, created_date, person, name, level, state) VALUES(
        uuid_generate_v1(), r.created_date, r.person, r.name, r.level,
        '{"type":"' || r.name || '","person":"' || r.person || '","builds":[]}');
    END LOOP;

    DELETE FROM person_badge WHERE uuid=r.uuid;
  END LOOP;
END$$;

ALTER TABLE person_badge DROP count;

ALTER TABLE person_badge ALTER state SET NOT NULL;

--//@UNDO

ALTER TABLE person_badge ADD count INT;

INSERT into person_badge(uuid, created_date, name, person, level, count)
  SELECT uuid_generate_v1(), min(created_date) as "created_date", name, person, level, count(level) from person_badge group by name, person, level;

ALTER TABLE person_badge ALTER count SET NOT NULL;

ALTER TABLE person_badge DROP state;

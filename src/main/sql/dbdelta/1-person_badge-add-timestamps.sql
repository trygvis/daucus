ALTER TABLE person_badge ADD timestamps TIMESTAMP[];

DO $$
DECLARE r RECORD;
DECLARE ts TIMESTAMP[];
BEGIN
  FOR r IN SELECT uuid, count FROM person_badge
  LOOP
    SELECT array_agg(x) FROM (SELECT current_timestamp FROM generate_series(1, r.count)) AS x INTO ts;
    UPDATE person_badge SET timestamps=ts WHERE uuid=r.uuid;
  END LOOP;
END$$;

ALTER TABLE person_badge DROP count;

--//@UNDO

ALTER TABLE person_badge ADD count INT;
UPDATE person_badge SET count=array_length(timestamps, 1);
ALTER TABLE person_badge ALTER count SET NOT NULL;
ALTER TABLE person_badge DROP timestamps;

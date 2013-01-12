ALTER TABLE jenkins_server ADD name VARCHAR(100);

UPDATE jenkins_server SET name = regexp_replace(url, '^https?://([^/]*).*', '\1');

--//@UNDO

ALTER TABLE jenkins_server DROP name;

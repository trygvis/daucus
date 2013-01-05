BEGIN;

INSERT INTO jenkins_server (uuid, created_date, url, enabled) VALUES
('782a75f6-40a4-11e2-aca6-20cf30557fa0', CURRENT_TIMESTAMP, 'https://builds.apache.org', FALSE),
('4c473c86-40ad-11e2-ae61-20cf30557fa0', CURRENT_TIMESTAMP, 'http://ci.jruby.org', FALSE),
('518c6162-411b-11e2-b63c-20cf30557fa0', CURRENT_TIMESTAMP, 'http://www.simantics.org/jenkins', FALSE),
('3c1a1448-422c-11e2-a7b3-20cf30557fa0', CURRENT_TIMESTAMP, 'https://jenkins.puppetlabs.com', FALSE);

-- apache jenkins: '782a75f6-40a4-11e2-aca6-20cf30557fa0'
-- olamy: '8588a612-4b5a-11e2-879d-20cf30557fa0'

INSERT INTO person (uuid, created_date, name) VALUES ('8588a612-4b5a-11e2-879d-20cf30557fa0', CURRENT_TIMESTAMP, 'Olivier Lamy');
INSERT INTO person_jenkins_user(person, jenkins_user) VALUES('8588a612-4b5a-11e2-879d-20cf30557fa0', 'e35f81ae-7589-4644-ad90-198a6bc582f8');

COMMIT;

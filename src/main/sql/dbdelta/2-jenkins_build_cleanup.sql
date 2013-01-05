ALTER TABLE jenkins_build DROP result;
ALTER TABLE jenkins_build DROP number;
ALTER TABLE jenkins_build DROP duration;
ALTER TABLE jenkins_build DROP timestamp;

--//@UNDO

ALTER TABLE jenkins_build ADD result VARCHAR(100);
ALTER TABLE jenkins_build ADD number INT;
ALTER TABLE jenkins_build ADD duration INT;
ALTER TABLE jenkins_build ADD TIMESTAMP TIMESTAMP;

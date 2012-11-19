-- Test data for my projects at gitorious.org
BEGIN;

INSERT INTO gitorious_project VALUES ('esper-test-project');
INSERT INTO gitorious_repository VALUES ('esper-test-project', 'esper-test-project', 'https://gitorious.org/esper-test-project/esper-test-project.atom');

INSERT INTO subscriber VALUES ('trygvis');
INSERT INTO subscription_gitorious_repository VALUES ('trygvis', 'esper-test-project', 'esper-test-project');

COMMIT;
- 

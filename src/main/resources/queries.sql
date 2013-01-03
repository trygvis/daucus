-- All unclaimed jenkins users
select absolute_url from jenkins_user where uuid not in (select jenkins_user from person_jenkins_user);

'use strict';

function JenkinsServer($resource) {
  return $resource('/resource/jenkins/server/:uuid', {uuid: '@uuid'});
}

angular.
    module('jenkinsServer', ['ngResource']).
    factory('JenkinsServer', JenkinsServer);

function JenkinsJob($resource) {
  return $resource('/resource/jenkins/job/:uuid', {uuid: '@uuid'});
}

angular.
    module('jenkinsJob', ['ngResource']).
    factory('JenkinsJob', JenkinsJob);

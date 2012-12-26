'use strict';

function JenkinsServerService($resource) {
  return $resource('/resource/jenkins/server/:uuid', {uuid: '@uuid'});
}

angular.
    module('jenkinsServerService', ['ngResource']).
    factory('JenkinsServerService', JenkinsServerService);

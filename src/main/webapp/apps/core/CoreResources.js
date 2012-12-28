'use strict';

function Person($resource) {
  return $resource('/resource/core/person/:uuid', {uuid: '@uuid'});
}

angular.
    module('person', ['ngResource']).
    factory('Person', Person);

function Build($resource) {
  return $resource('/resource/core/build/:uuid', {uuid: '@uuid'});
}

angular.
    module('build', ['ngResource']).
    factory('Build', Build);

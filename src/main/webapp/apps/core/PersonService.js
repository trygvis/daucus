'use strict';

function PersonService($resource) {
  return $resource('/resource/core/person/:uuid', {uuid: '@uuid'});
}

angular.
    module('personService', ['ngResource']).
    factory('PersonService', PersonService);

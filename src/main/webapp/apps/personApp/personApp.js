'use strict';

var personApp = angular.module('personApp', ['personService']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: PersonCtrl, templateUrl: '/apps/personApp/person.html?noCache=' + noCache});
});

function PersonCtrl($scope, $location, PersonService) {
  PersonService.get({uuid: uuid}, function (person) {
    $scope.person = person;
  });
}

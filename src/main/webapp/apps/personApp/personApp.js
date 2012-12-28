'use strict';

var personApp = angular.module('personApp', ['person', 'build']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: PersonCtrl, templateUrl: '/apps/personApp/person.html?noCache=' + noCache});
});

function PersonCtrl($scope, $location, Person, Build) {
  Person.get({uuid: uuid}, function (person) {
    $scope.person = person;
  });

  Build.query({person: uuid}, function (builds) {
    $scope.builds = builds;
  });
}

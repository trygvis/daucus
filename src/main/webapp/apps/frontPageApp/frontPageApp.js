'use strict';

var frontPageApp = angular.module('frontPageApp', ['personService']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: FrontPageCtrl, templateUrl: '/apps/frontPageApp/frontPage.html?noCache=' + noCache});
});

function FrontPageCtrl($scope, $location, PersonService) {
  PersonService.query(function (persons) {
    $scope.persons = persons;
  });
}

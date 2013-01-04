'use strict';

var frontPageApp = angular.module('frontPageApp', ['ngGrid', 'person', 'pagingTableService', 'core.directives']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: FrontPageCtrl, templateUrl: '/apps/frontPageApp/frontPage.html?noCache=' + noCache});
});

function FrontPageCtrl($scope, Person, PagingTableService) {
  $scope.persons = PagingTableService.create($scope, PagingTableService.defaultCallback(Person));
}

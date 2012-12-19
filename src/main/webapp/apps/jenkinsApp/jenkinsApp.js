'use strict';

var jenkinsApp = angular.module('jenkinsApp', ['jenkinsServerService']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: ServerListCtrl, templateUrl: '/apps/jenkinsApp/server-list.html?noCache=' + noCache});
  $routeProvider.
      when('/server/:uuid', {controller: ServerCtrl, templateUrl: '/apps/jenkinsApp/server.html?noCache=' + noCache});
//  $routeProvider.otherwise({ redirectTo: '/' });

  // This fucks shit up
//  $locationProvider.html5Mode(true);
});

function ServerListCtrl($scope, $route, $routeParams, $location, JenkinsServerService) {
  JenkinsServerService.query(function (servers) {
    $scope.servers = servers;
  });

  $scope.showServer = function (uuid) {
    $location.path('/server/' + uuid);
  };
}

function ServerCtrl($scope, $routeParams, JenkinsServerService) {
  window.x = $routeParams;
  JenkinsServerService.get({uuid: $routeParams.uuid}, function (server) {
    $scope.server = server;
  });
}

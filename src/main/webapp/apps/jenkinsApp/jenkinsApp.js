'use strict';

var jenkinsApp = angular.module('jenkinsApp', ['jenkinsServer', 'jenkinsJob', 'pagingTableService']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: ServerListCtrl, templateUrl: '/apps/jenkinsApp/server-list.html?noCache=' + noCache});
  $routeProvider.
      when('/server/:uuid', {controller: ServerCtrl, templateUrl: '/apps/jenkinsApp/server.html?noCache=' + noCache});
//  $routeProvider.otherwise({ redirectTo: '/' });

  // This fucks shit up
//  $locationProvider.html5Mode(true);
});

function ServerListCtrl($scope, $location, JenkinsServer) {
  JenkinsServer.query(function (servers) {
    $scope.servers = servers;
  });

  $scope.showServers = function () {
    $location.path('/');
  };

  $scope.showServer = function (uuid) {
    $location.path('/server/' + uuid);
  };
}

function ServerCtrl($scope, $location, $routeParams, JenkinsServer, JenkinsJob, PagingTableService) {
  var serverUuid = $routeParams.uuid;

  JenkinsServer.get({uuid: serverUuid}, function (server) {
    $scope.server = server;
  });

  $scope.jobs = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsJob, {server: serverUuid}));

  $scope.showServers = function () {
    $location.path('/');
  };

  $scope.showServer = function (uuid) {
    $location.path('/server/' + uuid);
  };
}

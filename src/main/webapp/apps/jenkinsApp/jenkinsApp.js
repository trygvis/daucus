'use strict';

var jenkinsApp = angular.module('jenkinsApp', ['jenkinsServer', 'jenkinsJob', 'jenkinsBuild', 'pagingTableService']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: ServerListCtrl, templateUrl: '/apps/jenkinsApp/server-list.html?noCache=' + noCache}).
      when('/server/:uuid', {controller: ServerCtrl, templateUrl: '/apps/jenkinsApp/server.html?noCache=' + noCache}).
      when('/job/:uuid', {controller: JobCtrl, templateUrl: '/apps/jenkinsApp/job.html?noCache=' + noCache}).
      when('/build/:uuid', {controller: BuildCtrl, templateUrl: '/apps/jenkinsApp/build.html?noCache=' + noCache});
});

function ServerListCtrl($scope, $location, JenkinsServer) {
  JenkinsServer.query(function (servers) {
    $scope.servers = servers;
  });

  $scope.showServers = function () { $location.path('/'); };
  $scope.showServer = function (uuid) { $location.path('/server/' + uuid); };
}

function ServerCtrl($scope, $location, $routeParams, JenkinsServer, JenkinsJob, PagingTableService) {
  var serverUuid = $routeParams.uuid;

  JenkinsServer.get({uuid: serverUuid}, function (server) {
    $scope.server = server;
  });

  $scope.jobs = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsJob, {server: serverUuid}));

  $scope.showServers = function () { $location.path('/'); };
  $scope.showJob = function (uuid) { $location.path('/job/' + uuid); };
}

function JobCtrl($scope, $location, $routeParams, JenkinsJob, JenkinsBuild, PagingTableService) {
  var jobUuid = $routeParams.uuid;

  JenkinsJob.get({uuid: jobUuid}, function (details) {
    $scope.details = details;
  });

  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsBuild, {job: jobUuid}));

  $scope.showServers = function () { $location.path('/'); };
  $scope.showServer = function () { $location.path('/server/' + $scope.job.server); };
  $scope.showBuild = function (uuid) { $location.path('/build/' + uuid); };
}

function BuildCtrl($scope, $location, $routeParams, JenkinsBuild) {
  var buildUuid = $routeParams.uuid;

  JenkinsBuild.get({uuid: buildUuid}, function (details) {
    $scope.details = details;
  });

  $scope.showServers = function () { $location.path('/'); };
  $scope.showServer = function (uuid) { $location.path('/server/' + $scope.server.uuid); };
  $scope.showJob = function (uuid) { $location.path('/job/' + $scope.build.job); };
}

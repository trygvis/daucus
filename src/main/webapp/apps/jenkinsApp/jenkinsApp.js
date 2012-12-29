'use strict';

var jenkinsApp = angular.module('jenkinsApp', ['jenkinsServer', 'jenkinsJob', 'jenkinsBuild', 'pagingTableService']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: ServerListCtrl, templateUrl: '/apps/jenkinsApp/server-list.html?noCache=' + noCache}).
      when('/server/:uuid', {controller: ServerCtrl, templateUrl: '/apps/jenkinsApp/server.html?noCache=' + noCache}).
      when('/job/:uuid', {controller: JobCtrl, templateUrl: '/apps/jenkinsApp/job.html?noCache=' + noCache});
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

  JenkinsJob.get({uuid: jobUuid}, function (job) {
    $scope.job = job;
  });

  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsBuild, {job: jobUuid}));

  $scope.showServers = function () { $location.path('/'); };
  $scope.showServer = function (uuid) { $location.path('/server/' + $scope.job.server); };
}

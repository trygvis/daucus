'use strict';

var jenkinsApp = angular.module('jenkinsApp', ['jenkinsServer', 'jenkinsJob', 'jenkinsBuild', 'core.directives', 'pagingTableService']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: ServerListCtrl, templateUrl: '/apps/jenkinsApp/server-list.html?noCache=' + noCache}).
      when('/server/:serverUuid', {controller: ServerCtrl, templateUrl: '/apps/jenkinsApp/server.html?noCache=' + noCache}).
      when('/server/:serverUuid/job/:jobUuid', {controller: JobCtrl, templateUrl: '/apps/jenkinsApp/job.html?noCache=' + noCache}).
      when('/server/:serverUuid/job/:jobUuid/build/:buildUuid', {controller: BuildCtrl, templateUrl: '/apps/jenkinsApp/build.html?noCache=' + noCache});
});

function ServerListCtrl($scope, $location, JenkinsServer) {
  JenkinsServer.query(function (servers) {
    $scope.servers = servers;
  });
}

function ServerCtrl($scope, $location, $routeParams, JenkinsServer, JenkinsJob, PagingTableService) {
  $scope.serverUuid = $routeParams.serverUuid;

  JenkinsServer.get({uuid: $scope.serverUuid}, function (server) {
    $scope.server = server;
  });

  $scope.jobs = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsJob, {server: $scope.serverUuid}));
}

function JobCtrl($scope, $location, $routeParams, JenkinsJob, JenkinsBuild, PagingTableService) {
  $scope.serverUuid = $routeParams.serverUuid;
  $scope.jobUuid = $routeParams.jobUuid;

  JenkinsJob.get({uuid: $scope.jobUuid}, function (details) {
    $scope.details = details;
  });

  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsBuild, {job: $scope.jobUuid, orderBy: "timestamp-"}));
}

function BuildCtrl($scope, $location, $routeParams, JenkinsBuild) {
  $scope.serverUuid = $routeParams.serverUuid;
  $scope.jobUuid = $routeParams.jobUuid;
  $scope.buildUuid = $routeParams.buildUuid;

  JenkinsBuild.get({uuid: $scope.buildUuid}, function (details) {
    $scope.details = details;
  });
}

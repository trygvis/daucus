'use strict';

function NavbarService() {
  var create = function(tabs) {
    console.log("create", tabs);
    var currentIndex = 0;
    var currentTab = tabs[currentIndex];

    var onClick = function(tab) {
      console.log("onClick", arguments);
      currentTab = tab;
      currentIndex = _.indexOf(tabs, tab);
    };

    var selected = function() {
      return currentTab;
    };

    var selectedIndex = function() {
      return currentIndex;
    };

    return {
      onClick: onClick,
      selected: selected,
      selectedIndex: selectedIndex,
      tabs: tabs
    }
  };

  return {
    create: create
  }
}

angular.
    module('navbarService', ['ngResource']).
    factory('NavbarService', NavbarService);

var jenkinsApp = angular.module('jenkinsApp', ['jenkinsServer', 'jenkinsJob', 'jenkinsBuild', 'core.directives', 'navbarService', 'pagingTableService']).config(function ($routeProvider) {
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

function ServerCtrl($scope, $routeParams, JenkinsServer, JenkinsJob, PagingTableService, NavbarService) {
  $scope.serverUuid = $routeParams.serverUuid;

  JenkinsServer.get({uuid: $scope.serverUuid}, function (server) {
    $scope.server = server;
  });

  $scope.jobs = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsJob, {server: $scope.serverUuid}));

  $scope.navbar = NavbarService.create(["Overview", "Jobs", "Recent Builds"]);
}

function JobCtrl($scope, $location, $routeParams, JenkinsJob, JenkinsBuild, PagingTableService) {
  $scope.serverUuid = $routeParams.serverUuid;
  $scope.jobUuid = $routeParams.jobUuid;

  JenkinsJob.get({uuid: $scope.jobUuid}, function (details) {
    $scope.details = details;
  });

  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsBuild, {job: $scope.jobUuid, orderBy: "created_date-"}));
}

function BuildCtrl($scope, $location, $routeParams, JenkinsBuild) {
  $scope.serverUuid = $routeParams.serverUuid;
  $scope.jobUuid = $routeParams.jobUuid;
  $scope.buildUuid = $routeParams.buildUuid;

  JenkinsBuild.get({uuid: $scope.buildUuid}, function (details) {
    $scope.details = details;
  });
}

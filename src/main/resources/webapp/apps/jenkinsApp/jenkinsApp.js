'use strict';

function NavTabsService($location) {
  var create = function(name, tabs) {
    var keys = _.map(tabs, function(element) {
      return element.toLowerCase().replace(' ', '-');
    });

    var currentKey = $location.search()[name] || "";
    var currentIndex = _.indexOf(keys, currentKey);
    currentIndex = currentIndex != -1 ? currentIndex : 0;
    var currentTab = tabs[currentIndex];

    var onClick = function(tab) {
      currentTab = tab;
      currentIndex = _.indexOf(tabs, tab);
      $location.search(name, keys[currentIndex]);
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
    module('navTabsService', ['ngResource']).
    factory('NavTabsService', NavTabsService);

var jenkinsApp = angular.module('jenkinsApp', ['jenkinsServer', 'jenkinsJob', 'jenkinsBuild', 'core.directives', 'navTabsService', 'pagingTableService']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: ServerListCtrl, templateUrl: '/apps/jenkinsApp/server-list.html?noCache=' + noCache}).
      when('/server/:serverUuid', {controller: ServerCtrl, templateUrl: '/apps/jenkinsApp/server.html?noCache=' + noCache, reloadOnSearch: false}).
      when('/server/:serverUuid/job/:jobUuid', {controller: JobCtrl, templateUrl: '/apps/jenkinsApp/job.html?noCache=' + noCache}).
      when('/server/:serverUuid/job/:jobUuid/build/:buildUuid', {controller: BuildCtrl, templateUrl: '/apps/jenkinsApp/build.html?noCache=' + noCache});
});

function ServerListCtrl($scope, $location, JenkinsServer) {
  JenkinsServer.query(function (servers) {
    $scope.servers = servers;
  });
}

function ServerCtrl($scope, $routeParams, JenkinsServer, JenkinsJob, JenkinsBuild, PagingTableService, NavTabsService) {
  $scope.serverUuid = $routeParams.serverUuid;

  $scope.navTabs = NavTabsService.create("view", ["Overview", "Jobs", "Recent Builds"]);

  JenkinsServer.get({uuid: $scope.serverUuid}, function (server) {
    $scope.server = server;
  });

  $scope.jobs = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsJob, {server: $scope.serverUuid, orderBy: "display_name"}));

  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(JenkinsBuild, {server: $scope.serverUuid, orderBy: "created_date-"}));
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

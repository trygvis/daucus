'use strict';

var frontPageApp = angular.module('frontPageApp', ['ngGrid', 'person', 'badge', 'build', 'jenkinsUser', 'jenkinsBuild', 'pagingTableService', 'core.directives']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: FrontPageCtrl, templateUrl: '/apps/frontPageApp/frontPage.html?noCache=' + noCache}).
      when('/badge/', {controller: BadgeListCtrl, templateUrl: '/apps/frontPageApp/badgeList.html?noCache=' + noCache}).
      when('/badge/:badgeUuid', {controller: BadgeCtrl, templateUrl: '/apps/frontPageApp/badge.html?noCache=' + noCache}).
      when('/person/', {controller: PersonListCtrl, templateUrl: '/apps/frontPageApp/personList.html?noCache=' + noCache, reloadOnSearch: false}).
      when('/person/:personUuid', {controller: PersonCtrl, templateUrl: '/apps/frontPageApp/person.html?noCache=' + noCache}).
      when('/build/', {controller: BuildListCtrl, templateUrl: '/apps/frontPageApp/buildList.html?noCache=' + noCache}).
      when('/build/:buildUuid', {controller: BuildCtrl, templateUrl: '/apps/frontPageApp/build.html?noCache=' + noCache});
});

function FrontPageCtrl($scope, Person, Badge) {
  $scope.persons = Person.query();
  $scope.recentBadges = Badge.query();
}

function groupBy(array, size) {
  var group = [];
  var groups = [];
  angular.forEach(array, function (element) {
    group.push(element);
    if (group.length == size) {
      groups.push(group);
      group = [];
    }
  });

  if (group.length != 0) {
    groups.push(group);
  }
  return groups;
}

function groupByDay(array, accessor) {
  var withDay = _.map(array, function(item) {
    item.day = new Date(accessor(item)).clearTime().getTime();
    return item;
  });

  var byDay = _.groupBy(withDay, 'day');

  byDay = _.map(byDay, function(value, key) {
    var o = {};
    o[key] = value;
    return o;
  });

  return byDay;
}

function BadgeListCtrl($scope, Badge, PagingTableService) {
  var personsWatcher = function () {
    $scope.badgeGroups = groupByDay($scope.badges.rows, function (badge) { return badge.badge.createdDate });
  };

  $scope.badges = PagingTableService.create($scope, PagingTableService.defaultCallback(Badge, {orderBy: "created_date-"}),
      {count: 20, watcher: personsWatcher});

  $scope.badgeGroups = [];
}

function BadgeCtrl($scope, $routeParams, Badge) {
  var badgeUuid = $routeParams.badgeUuid;
  Badge.get({uuid: badgeUuid}, function (badge) {
    $scope.badge = badge;
  });
}

function PersonListCtrl($scope, $location, Person, PagingTableService) {
  var groupSize = 4, rows = 6;
  var personsWatcher = function () {
    $scope.personGroups = groupBy($scope.persons.rows, groupSize);
  };

  var page = $location.search().page || 1;
  var count = groupSize * rows;
  $scope.personGroups = [];
  $scope.persons = PagingTableService.create($scope, PagingTableService.defaultCallback(Person, {orderBy: "name"}),
      {startIndex: (page - 1) * count, count: count, watcher: personsWatcher});

  $scope.$watch("persons.currentPage()", function(newValue) {
    $location.search('page', newValue > 1 ? newValue : null);
  });
}

function PersonCtrl($scope, $routeParams, Person, Build, JenkinsUser, PagingTableService) {
  var personUuid = $routeParams.personUuid;

  $scope.mode = 'overview';

  var watcher = function () {
    $scope.buildGroups = groupByDay($scope.builds.rows, function(build) { return build.timestamp});
    console.log("$scope.buildGroups", $scope.buildGroups);
  };
  $scope.buildGroups = [];
  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(Build, {person: personUuid, orderBy: "timestamp-"}),
      {count: 50, watcher: watcher});

  $scope.setMode = function(mode) {
    $scope.mode = mode;
    switch(mode) {
      case 'builds':
        var builds = $scope.builds;

        if (builds.rows.length == 0) {
          $scope.builds.first();
        }
        break;
    }
  };

  Person.get({uuid: personUuid}, function (person) {
    $scope.person = person;

    $scope.jenkinsUsers = person.jenkinsUsers;
    _.forEach(person.jenkinsUsers, function(uuid, i) {
      JenkinsUser.get({uuid: uuid}, function(user) {
        $scope.jenkinsUsers[i] = user;
      })});
  });

  Build.query({person: personUuid, orderBy: "timestamp-"}, function (builds) {
    $scope.recentBuilds = builds;
  });
}

function BuildListCtrl($scope, Build, PagingTableService) {
  var watcher = function () {
    $scope.buildGroups = groupByDay($scope.builds.rows, function(build) { return build.build.timestamp});
  };

  $scope.buildGroups = [];
  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(Build, {fields: "detailed"}),
      { count: 100, watcher: watcher });
}

function BuildCtrl($scope, $routeParams, Build, JenkinsBuild) {
  var buildUuid = $routeParams.buildUuid;

  $scope.build = Build.get({uuid: buildUuid}, function (build) {
    $scope.jenkinsBuild = JenkinsBuild.get({uuid: build.build.buildUuid});
  });
}

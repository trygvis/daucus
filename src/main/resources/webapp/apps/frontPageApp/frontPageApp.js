'use strict';

var frontPageApp = angular.module('frontPageApp', ['ngGrid', 'person', 'badge', 'build', 'jenkinsUser', 'pagingTableService', 'core.directives']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: FrontPageCtrl, templateUrl: '/apps/frontPageApp/frontPage.html?noCache=' + noCache}).
      when('/badge/', {controller: BadgeListCtrl, templateUrl: '/apps/frontPageApp/badgeList.html?noCache=' + noCache}).
      when('/badge/:badgeUuid', {controller: BadgeCtrl, templateUrl: '/apps/frontPageApp/badge.html?noCache=' + noCache}).
      when('/person/', {controller: PersonListCtrl, templateUrl: '/apps/frontPageApp/personList.html?noCache=' + noCache}).
      when('/person/:personUuid', {controller: PersonCtrl, templateUrl: '/apps/frontPageApp/person.html?noCache=' + noCache}).
      when('/build/', {controller: BuildListCtrl, templateUrl: '/apps/frontPageApp/buildList.html?noCache=' + noCache}).
      when('/build/:buildUuid', {controller: BuildCtrl, templateUrl: '/apps/frontPageApp/build.html?noCache=' + noCache});
  // job/:jobUuid/build/:buildUuid
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

function BadgeListCtrl($scope, Badge, PagingTableService) {
  var personsWatcher = function () {
    var withDay = _.map($scope.badges.rows, function(badge) {
      badge.day = new Date(badge.badge.createdDate).clearTime().getTime();
//      badge.day.clearTime();
      return badge;
    });

    var byDay = _.groupBy(withDay, 'day');
//    console.log("byDay", byDay);

    byDay = _.map(byDay, function(value, key) {
      var o = {};
      o[key] = value;
      return o;
    });

//    byDay = _.toArray(byDay).reverse();
//    console.log("byDay", byDay);

    $scope.badgeGroups = byDay;
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

function PersonListCtrl($scope, Person, PagingTableService) {
  var groupSize = 4, rows = 6;
  var personsWatcher = function () {
    $scope.personGroups = groupBy($scope.persons.rows, groupSize);
  };

  $scope.persons = PagingTableService.create($scope, PagingTableService.defaultCallback(Person, {orderBy: "name"}),
      {count: groupSize * rows, watcher: personsWatcher});

  console.log("$scope.persons.searchText", $scope.persons.searchText);
  console.log("$scope.persons.rows", $scope.persons.rows);

  $scope.personGroups = [];
}

function PersonCtrl($scope, $routeParams, Person, Build, JenkinsUser, PagingTableService) {
  var personUuid = $routeParams.personUuid;

  $scope.mode = 'overview';
  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(Build, {person: personUuid}), {count: 100});

  $scope.setMode = function(mode) {
    $scope.mode = mode;
    switch(mode) {
      case 'builds':
        var builds = $scope.builds;

        console.log("$scope.builds.length=" + builds.rows.length);
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

  Build.query({person: personUuid}, function (builds) {
    $scope.recentBuilds = builds;
  });
}

function BuildListCtrl($scope, Build, PagingTableService) {
  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(Build, {fields: "detailed"}));
}

function BuildCtrl($scope, $routeParams, Build, PagingTableService) {
  var buildUuid = $routeParams.buildUuid;

  Build.get({uuid: buildUuid}, function (build) {
    $scope.build = build;
  });
}

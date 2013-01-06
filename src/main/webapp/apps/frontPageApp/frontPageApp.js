'use strict';

var frontPageApp = angular.module('frontPageApp', ['ngGrid', 'person', 'badge', 'build', 'pagingTableService', 'core.directives']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: FrontPageCtrl, templateUrl: '/apps/frontPageApp/frontPage.html?noCache=' + noCache}).
      when('/badge/', {controller: BadgeListCtrl, templateUrl: '/apps/frontPageApp/badgeList.html?noCache=' + noCache}).
      when('/person/', {controller: PersonListCtrl, templateUrl: '/apps/frontPageApp/personList.html?noCache=' + noCache}).
      when('/person/:personUuid', {controller: PersonCtrl, templateUrl: '/apps/frontPageApp/person.html?noCache=' + noCache});
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
  var groupSize = 6;

  var personsWatcher = function () {
    var withDay = _.map($scope.badges.rows, function(badge) {
      badge.day = new Date(badge.badge.createdDate).clearTime().getTime();
//      badge.day.clearTime();
      return badge;
    });

    var byDay = _.groupBy(withDay, 'day');
    console.log("byDay", byDay);
//    var dateGroups = _.map(byDay, function(group, date) {
//      return {date: groupBy(group, groupSize)}
//    });

    $scope.badgeGroups = byDay;
  };

  $scope.badges = PagingTableService.create($scope, PagingTableService.defaultCallback(Badge),
      {count: groupSize * 6, watcher: personsWatcher});

  $scope.badgeGroups = [];
}

function PersonListCtrl($scope, Person, PagingTableService) {
  var groupSize = 4;
  var personsWatcher = function () {
    $scope.personGroups = groupBy($scope.persons.rows, groupSize);
  };

  $scope.persons = PagingTableService.create($scope, PagingTableService.defaultCallback(Person, {orderBy: "name"}),
      {count: groupSize * 6, watcher: personsWatcher});

  $scope.personGroups = [];
}

function PersonCtrl($scope, $routeParams, Person, Build, PagingTableService) {
  var personUuid = $routeParams.personUuid;

  $scope.mode = 'overview';
  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(Build, {person: personUuid}));

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
  });

  Build.query({person: personUuid}, function (builds) {
    $scope.recentBuilds = builds;
  });
}

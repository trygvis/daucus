'use strict';

var frontPageApp = angular.module('frontPageApp', ['ngGrid', 'person', 'badge', 'build', 'pagingTableService', 'core.directives']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: FrontPageCtrl, templateUrl: '/apps/frontPageApp/frontPage.html?noCache=' + noCache}).
      when('/person/', {controller: PersonListCtrl, templateUrl: '/apps/frontPageApp/personList.html?noCache=' + noCache}).
      when('/person/:personUuid', {controller: PersonCtrl, templateUrl: '/apps/frontPageApp/person.html?noCache=' + noCache});
});

function FrontPageCtrl($scope, Person, Badge) {
  $scope.persons = Person.query();
  $scope.recentBadges = Badge.query();
}

function PersonListCtrl($scope, Person, PagingTableService) {
  var personsWatcher = function () {
    var array = $scope.persons.rows;

    var group = [];
    var groups = [];
    angular.forEach(array, function(element) {
      group.push(element);
      if(group.length == 4) {
        groups.push(group);
        group = [];
      }
    });

    if(group.length != 0) {
      groups.push(group);
    }

    $scope.personGroups = groups;
  };

  $scope.persons = PagingTableService.create($scope, PagingTableService.defaultCallback(Person, {orderBy: "name"}), {count: 4 * 6, watcher: personsWatcher});

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

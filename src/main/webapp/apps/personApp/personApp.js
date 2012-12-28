'use strict';

var personApp = angular.module('personApp', ['person', 'build']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: PersonCtrl, templateUrl: '/apps/personApp/person.html?noCache=' + noCache});
});

function PersonCtrl($scope, $location, Person, Build) {
  function queryBuilds() {
    var builds = $scope.builds;
    Build.query({person: uuid, startIndex: builds.startIndex, count: builds.count}, function (builds) {
      $scope.builds.rows = builds;
    });
  }

  $scope.mode = 'overview';
  $scope.builds = {
    rows: [],
    startIndex: 0,
    count: 10,
    first: function() {
      $scope.builds.startIndex = 0;
      queryBuilds();
    },
    next: function() {
      $scope.builds.startIndex += $scope.builds.count;
      queryBuilds();
    },
    prev: function() {
      if($scope.builds.startIndex == 0) {
        return;
      }
      $scope.builds.startIndex -= $scope.builds.count;
      queryBuilds();
    }
  };

  $scope.setMode = function(mode) {
    $scope.mode = mode;
    switch(mode) {
      case 'builds':
        var builds = $scope.builds;

        console.log("$scope.builds.length=" + builds.rows.length);
        if (builds.rows.length == 0) {
          queryBuilds(builds);
        }
        break;
    }
  };

  Person.get({uuid: uuid}, function (person) {
    $scope.person = person;
  });

  Build.query({person: uuid}, function (builds) {
    $scope.recentBuilds = builds;
  });
}

'use strict';

var personApp = angular.module('personApp', ['person', 'build', 'pagingTableService']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: PersonCtrl, templateUrl: '/apps/personApp/person.html?noCache=' + noCache});
});

function PersonCtrl($scope, $routeParams, Person, Build, PagingTableService) {
  var personUuid = uuid;

  $scope.mode = 'overview';

  $scope.builds = PagingTableService.create($scope, PagingTableService.defaultCallback(Build, {person: personUuid}));

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

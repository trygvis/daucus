'use strict';

var frontPageApp = angular.module('frontPageApp', ['ngGrid', 'person', 'badge', 'build', 'pagingTableService', 'core.directives']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: FrontPageCtrl, templateUrl: '/apps/frontPageApp/frontPage.html?noCache=' + noCache}).
      when('/person/:personUuid', {controller: PersonCtrl, templateUrl: '/apps/frontPageApp/person.html?noCache=' + noCache});
});

function FrontPageCtrl($scope, Person, Badge, PagingTableService) {
  $scope.persons = PagingTableService.create($scope, PagingTableService.defaultCallback(Person));
  $scope.recentBadges = Badge.query();
}

function PersonCtrl($scope, $routeParams, Person, Badge, Build, PagingTableService) {
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

  Badge.query({person: personUuid}, function (badges) {
    $scope.badges = badges;
  });
}

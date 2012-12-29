'use strict';

var buildApp = angular.module('buildApp', ['build', 'buildParticipant']).config(function ($routeProvider) {
  $routeProvider.
      when('/', {controller: BuildCtrl, templateUrl: '/apps/buildApp/build.html?noCache=' + noCache});
});

function BuildCtrl($scope, Build, BuildParticipant) {
  Build.get({uuid: uuid}, function(build) {
    window.build = $scope.build = build;
  });
  BuildParticipant.query({uuid: uuid}, function(persons) {
    $scope.participants = persons;
  });
}

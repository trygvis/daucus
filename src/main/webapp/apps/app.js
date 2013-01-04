var directives = angular.module('core.directives', []);

directives.directive('navbar', function () {
  return {
    restrict: 'E',
    templateUrl: '/apps/core/navbar.html'
  };
});

var directives = angular.module('core.directives', []);

directives.directive('navbar', function () {
  return {
    restrict: 'E',
    templateUrl: '/apps/core/navbar.html?noCache=' + noCache
  };
});

directives.directive('badge', function() {
  return {
    restrict: 'E',
    scope: {
      badgeDetail: '=badgeDetail'
    },
    template: '<span class="badge-inverse badge-level-{{badgeDetail.badge.level}} badge"><strong style="padding-right: 0.3em">{{badgeDetail.badge.name}}</strong>' +
        '<i class="icon-user"></i></span>' +
        ' awarded to ' +
        '<a href="/#/person/{{badgeDetail.person.uuid}}">{{badgeDetail.person.name}}</a>'
  }
});

/*
 <!--
 <span class="badge-level-{{badge.level}} badge">{{badge.name}}</span>
 -->
 <strong>{{badge.name}}</strong>
 <!--
 <i class="icon-user ng-class: {{{1: 'badge-level-1', 2: 'badge-level-2', 3: 'badge-level-3'}[badge.level]}}"></i>
 -->
 <span class="badge-level-{{badge.level}} badge">
 <i class="icon-user"></i>
 </span>

 {{badge.createdDate | date:'medium'}}

*/
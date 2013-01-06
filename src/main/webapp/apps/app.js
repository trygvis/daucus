var directives = angular.module('core.directives', []);

directives.filter('countBadgeByLevel', function () {
  return function (badges) {
    // 5 levels
    var levels = [0, 0, 0, 0, 0];
    angular.forEach(badges, function(value, key){
      levels[value.level - 1]++;
    });
    return levels;
  }
});

directives.filter('gz', function () {
  return function (num) {
    if(angular.isArray(num)) {
      var out = [];
      angular.forEach(num, function(x){
        if(x > 0) {
          out.push(x);
        }
      });

      return out;
    }
    else if(angular.isNumber(num)) {
      return num > 0;
    }
    console.log("fail");
    return undefined;
  }
});

directives.directive('navbar', function () {
  return {
    restrict: 'E',
    templateUrl: '/apps/core/navbar.html?noCache=' + noCache
  };
});

directives.directive('badge', function () {
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

var directives = angular.module('core.directives', []);

directives.filter('countBadgeByLevel', function () {
  return function (badges) {
    // 5 levels
    var levels = [0, 0, 0, 0, 0];
    angular.forEach(badges, function (value, key) {
      levels[value.level - 1]++;
    });
    return levels;
  }
});

directives.filter('isodate', function () {
  return function (date) {
    return date.toISOString();
  }
});

directives.filter('gz', function () {
  return function (num) {
    if (angular.isArray(num)) {
      var out = [];
      angular.forEach(num, function (x) {
        if (x > 0) {
          out.push(x);
        }
      });

      return out;
    }
    else if (angular.isNumber(num)) {
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
    template: '<span class="badge-inverse badge-level-{{badgeDetail.badge.level}} badge">' +
        '  <strong style="padding-right: 0.3em">{{badgeDetail.badge.name}}</strong>' +
        '  <i class="icon-user"></i>' +
        '</span>' +
        ' awarded to ' +
        '<a href="/#/person/{{badgeDetail.person.uuid}}">{{badgeDetail.person.name}}</a>. ' +
        '<a href="/#/badge/{{badgeDetail.badge.uuid}}">More</a>'
  }
});

directives.directive('badgeSpan', function () {
  var template =
      '<span class="badge-inverse badge-level-{{badge.level}} badge">' +
      '  <strong style="padding-right: 0.3em">{{badge.name}}</strong>' +
      '  <i class="icon-user"></i>' +
      '</span>';

  return {
    restrict: 'E',
    scope: {
      badge: '=badge'
    },
    template: template
  }
});

directives.directive('personLink', function () {
  return {
    restrict: 'E',
    scope: {
      person: '=person'
    },
    template: '<a href="/#/person/{{person.uuid}}">{{person.name}}</a>'
  }
});

directives.directive('avatarXl', function () {
  return {
    restrict: 'E',
    scope: {
      person: '=person'
    },
    template: '<a href="#/person/{{person.uuid}}">' +
        '<img ng-src="{{person.gravatar}}?default=identicon" class="avatar-image avatar80" title="{{person.name}}"/>' +
        '</a>'
  }
});

directives.directive('dogtagXl', function () {
  return {
    restrict: 'EACM',
    scope: {
      person: '=person'
    },
    templateUrl: '/apps/dogtag-xl.html'
  }
});

directives.directive('spinner', function () {
  return function($scope, element, attr) {
    var opts = {
      lines: 13, // The number of lines to draw
      length: 7, // The length of each line
      width: 4, // The line thickness
      radius: 10, // The radius of the inner circle
      corners: 1, // Corner roundness (0..1)
      rotate: 0, // The rotation offset
      color: '#000', // #rgb or #rrggbb
      speed: 1, // Rounds per second
      trail: 60, // Afterglow percentage
      shadow: false, // Whether to render a shadow
      hwaccel: false, // Whether to use hardware acceleration
      className: attr.spinnerClass || 'spinner', // The CSS class to assign to the spinner
      zIndex: 2e9, // The z-index (defaults to 2000000000)
      top: attr.spinnerTop || 'auto', // Top position relative to parent in px
      left: attr.spinnerLeft || 'auto' // Left position relative to parent in px
    };

    console.log("attr.spinnerTop =", attr.spinnerTop, "attr.spinnerLeft =", attr.spinnerLeft);

    var target = element[0];
    new Spinner(opts).spin(target);
    return target;
  }
});

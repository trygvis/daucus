'use strict';

var frontPageApp = angular.module('frontPageApp', ['ngGrid', 'person']).config(function ($routeProvider, $locationProvider) {
  $routeProvider.
      when('/', {controller: FrontPageCtrl, templateUrl: '/apps/frontPageApp/frontPage.html?noCache=' + noCache});
});

function FrontPageCtrl($scope, $http, Person) {
  $scope.persons = [];

  $scope.pagingOptions = {
    pageSizes: [10],
    pageSize: 10,
    totalServerItems: 0,
    currentPage: 1
  };

  $scope.personsGridOptions = {
    data: 'persons',
    displayFooter: true,
    enablePaging: true,
    enableRowReordering: false,
    enableColumnReordering: false,
    showFilter: false,
    showColumnMenu: false,
    canSelectRows: false,
    displaySelectionCheckbox: false,
    pagingOptions: $scope.pagingOptions,
    columnDefs: [
      {
        field: 'name',
        displayName: 'Name',
        cellTemplate: '<a href="/person/{{row.getProperty(\'uuid\')}}">{{row.getProperty(col.field)}}</a>'
      },
      {
        field: 'badges',
        displayName: 'Badges',
        cellTemplate: '<div>{{row.getProperty(col.field).length}}</div>'
      }
    ]
  };

  $scope.setPagingData = function(data, page, pageSize){
//    $scope.persons = data.slice((page - 1) * pageSize, page * pageSize);
    $scope.persons = data;
//    $scope.personsGridOptions.totalServerItems = data.length;
    window.x = $scope.personsGridOptions;
    if (!$scope.$$phase) {
      $scope.$apply();
    }
  };

  $scope.getPagedDataAsync = function (pageSize, page/*, searchText*/) {
    setTimeout(function () {

      Person.query({startIndex: page * pageSize, count: pageSize}, function (persons) {
        $scope.setPagingData(persons, page, pageSize);
      });
    }, 100);
  };

  $scope.$watch('pagingOptions', function () {
    $scope.getPagedDataAsync($scope.pagingOptions.pageSize, $scope.pagingOptions.currentPage);
  }, true);

//  $http.get('/resource/core/person-count').success(function(count) {
//    $scope.pagingOptions.totalServerItems = count;
//
//    $scope.getPagedDataAsync($scope.pagingOptions.pageSize, $scope.pagingOptions.currentPage);
//  });
}

function PagingTableService() {
  var create = function ($scope, fetchCallback) {
    var self = {
      rows: [],
      startIndex: 0,
      count: 10
    };

    var update = function(){
      fetchCallback(self.startIndex, self.count, function(data) {
        self.rows = data.rows;
      });
    };

    self.first = function () {
      self.startIndex = 0;
      update();
    };

    self.next = function () {
      this.startIndex += this.count;
      update();
    };

    self.prev = function () {
      if (self.startIndex == 0) {
        return;
      }
      self.startIndex -= self.count;
      update();
    };

    // Do an initial fetch
    update();

    return self;
  };

  var defaultCallback = function(Resource, args) {
    args = args || {};
    return function(startIndex, count, cb) {
      console.log("fetching", arguments);
      args.startIndex = startIndex;
      args.count = count;
      Resource.query(args, function(data, headers) {
        var totalResults = headers("total-results");
        console.log("got data", arguments);
        console.log("totalResults", totalResults);
        cb({
          totalResults: totalResults,
          rows: data
        });
      });
    };
  };

  return {
    create: create,
    defaultCallback: defaultCallback
  }
}

angular.
    module('pagingTableService', ['ngResource']).
    factory('PagingTableService', PagingTableService);

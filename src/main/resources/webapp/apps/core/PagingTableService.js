function PagingTableService() {
  var create = function ($scope, fetchCallback, options) {
    options = options || {};
    var watcher = options.watcher || function(){};
    var self = {
      rows: [],
      query: "",
      startIndex: options.startIndex || 0,
      count: options.count || 10
    };

    var update = function(){
      fetchCallback(self.startIndex, self.count, self.query, function(data) {
        self.rows = data.rows;
        watcher();
      });
    };

    self.first = function () {
      self.startIndex = 0;
      update();
    };

    self.next = function () {
      self.startIndex += self.count;
      update();
    };

    self.prev = function () {
      if (self.startIndex == 0) {
        return;
      }
      self.startIndex -= self.count;
      update();
    };

    self.onSearch = function () {
      console.log("search: " + self.query);
      update();
    };

    self.onSearchChange = function () {
      console.log("search: " + self.query);
      update();
    };

    // Do an initial fetch
    update();

    return self;
  };

  var defaultCallback = function(Resource, args) {
    args = args || {};
    return function(startIndex, count, query, cb) {
      if(startIndex) {
        args.startIndex = startIndex;
      }
      if(count) {
        args.count = count;
      }
      if(query) {
        args.query = query;
      }
      console.log("fetching", args);
      Resource.query(args, function(data, headers) {
        var totalResults = headers("total-results");
        console.log("totalResults", totalResults);
        console.log("got data", data);
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

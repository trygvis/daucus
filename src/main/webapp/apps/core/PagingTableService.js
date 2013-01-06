function PagingTableService() {
  var create = function ($scope, fetchCallback, options) {
    options = options || {};
    var watcher = options.watcher || function(){};
    var self = {
      rows: [],
      startIndex: options.startIndex || 0,
      count: options.count
    };

    var update = function(){
      fetchCallback(self.startIndex, self.count, function(data) {
        self.rows = data.rows;
        watcher();
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
      if(startIndex) {
        args.startIndex = startIndex;
      }
      if(count) {
        args.count = count;
      }
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

function PagingTableService() {
  var create = function ($scope, fetchCallback, options) {
    options = options || {};
    var watcher = options.watcher || function(){};
    var self = {
      rows: [],
      query: "",
      startIndex: options.startIndex || 0,
      count: options.count || 10,
      currentlySearching: false,
      queryStart: 0
    };

    var update = function () {
      var query = self.query;
      if (self.currentlySearching) {
        console.log("query active, storing =", query);
        return;
      }
      self.currentlySearching = true;
      self.queryStart = new Date().getTime();

      // This will update the spinner if the user want to show it.
      var interval = setInterval(function () {
        $scope.$apply();
      }, 500);

      fetchCallback(self.startIndex, self.count, query, function (data) {
        var now = new Date().getTime();
        console.log("Query took " + (now - self.queryStart) + "ms");

        clearInterval(interval);

        self.rows = data.rows;
        self.currentlySearching = false;
        self.queryStart = 0;

        if(self.query != query) {
          console.log("Had a new query requested, sending. query =", query, ", self.query =", self.query);
          update();
        }

        watcher();
      });
    };

    self.first = function () {
      self.startIndex = 0;
      update();
    };

    self.next = function () {
      if (self.currentlySearching) {
        return;
      }
      self.startIndex += self.count;
      update();
    };

    self.prev = function () {
      if (self.currentlySearching) {
        return;
      }
      if (self.startIndex == 0) {
        return;
      }
      self.startIndex -= self.count;
      update();
    };

    /*
     * The search functions needs to know if there already is a search in progress and if so, do not send the search
     * before the previous one completes.
     */

    self.onSearch = function () {
      update();
    };

    self.onSearchChange = function () {
      update();
    };

    /*
     * UI State queries
     *
     * TODO: the results should only be shown if the last query was successful. Add an 'error' state too.
     */

    self.showSpinner = function () {
      return self.currentlySearching && new Date().getTime() - self.queryStart > 500;
    };

    self.showResults = function () {
      return !self.currentlySearching;
    };

    self.showPrev = function () {
      return self.startIndex > 0;
    };

    self.showNext = function () {
      return true;
    };

    self.nextDisabled = function () {
      return self.currentlySearching;
    };

    self.prevDisabled = function () {
      return self.currentlySearching;
    };

    // Do an initial fetch
    update();

    return self;
  };

  var defaultCallback = function(Resource, args) {
    args = args || {};
    return function(startIndex, count, query, cb) {
      if(startIndex || startIndex == 0) {
        args.startIndex = startIndex;
      }
      if(count) {
        args.count = count;
      }
      if(query || query == "") {
        args.query = query;
      }
      console.log("Fetching page. args =", args);
      Resource.query(args, function(data, headers) {
        var totalResults = headers("total-results");
        console.log("Total results =", totalResults, "Data =", data);
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

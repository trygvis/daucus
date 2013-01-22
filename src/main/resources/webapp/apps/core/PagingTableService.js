function PagingTableService() {
  var create = function ($scope, fetchCallback, options) {
    options = options || {};
    var watcher = options.watcher || function(){};
    // This is exposed to the view as accessible variables
    var self = {
      rows: [],
      query: "",
      startIndex: options.startIndex || 0,
      count: options.count || 10,
      error: undefined
    };

    var internal = {
      // either "loading", "data" or "error"
      state: "loading",
      currentlySearching: false,
      queryStart: 0
    };

    var update = function () {
      var query = self.query;
      if (internal.currentlySearching) {
        console.log("query active, storing =", query);
        return;
      }
      internal.currentlySearching = true;
      internal.queryStart = new Date().getTime();

      // This will update the spinner if the user want to show it.
      var interval = setInterval(function () {
        $scope.$apply();
      }, 500);

      fetchCallback(self.startIndex, self.count, query, function (data, error) {
        var now = new Date().getTime();
        console.log("Query took " + (now - internal.queryStart) + "ms");
        clearInterval(interval);

        if(typeof data !== "undefined") {
          internal.state = "data";
          internal.currentlySearching = false;
          internal.queryStart = 0;
          self.rows = data.rows;
          self.error = undefined;

          if(self.query != query) {
            console.log("Had a new query requested, sending. query =", query, ", self.query =", self.query);
            update();
          }

          watcher();
        }
        else {
          internal.state = "error";
          internal.currentlySearching = false;
          internal.queryStart = 0;
          // Here we should probably store the old rows.
          self.rows = [];
          self.error = {
            message: "wat!!"
          };

          watcher();
        }
      });
    };

    /*
     * UI actions
     */
    self.first = function () {
      self.startIndex = 0;
      update();
    };

    self.next = function () {
      if (internal.currentlySearching) {
        return;
      }
      self.startIndex += self.count;
      update();
    };

    self.prev = function () {
      if (internal.currentlySearching) {
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
      self.startIndex = 0;
      update();
    };

    /*
     * UI State queries
     */

    self.viewState = function() {
      return internal.state;
    };

    self.showPrev = function () {
      return self.startIndex > 0;
    };

    self.showNext = function () {
      return true;
    };

    self.nextDisabled = function () {
      return internal.currentlySearching;
    };

    self.prevDisabled = function () {
      return internal.currentlySearching;
    };

    self.currentPage = function() {
      return (self.startIndex / self.count) + 1;
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
        console.log("Total results =", totalResults, "data.length =", data.length, "Data =", data);
        cb({
          totalResults: totalResults,
          rows: data
        });
      }, function() {
        console.log("Failed");
        console.log(arguments);
        cb(undefined, {message: "Error loading data..."});
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

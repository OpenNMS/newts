
(function() {
    'use strict';

    angular
        .module('newts.resourceSearch', ['angularjs-dropdown-multiselect'])
        .directive('resourceSearch', function() {
            search.$inject = [ '$scope', '$http', 'graphService', 'siteConfig' ];
            return {
                restrict: 'E',
                templateUrl: 'resource-search.html',
                controller: search,
                controllerAs: 'vm',
                scope: {
                    onGraphSelect: '&'
                }
            };
        });

    function url(host, port, queryString) {
        return 'http://'+host+':'+port+'/search?q='+queryString;
    }

    function search($scope, $http, graphService, siteConfig) {
        /* jshint validthis: true */
        var vm = this;

        vm.resources = [];
        vm.metrics = {};
        vm.selected = {};
        vm.selectSettings = { externalIdProp: '' };
        vm.elapsed = -1;

        vm.graph = graph;
        vm.submit = submit;
        
        function graph(resource) {
            var labels = [];
            vm.selected[resource].forEach(function(m) {
                labels.push(m.label);
            });
            graphService.broadcast({'id': resource, 'metrics': labels});
            vm.selected[resource] = [];
            $scope.onGraphSelect();
        }

        function submit() {
            var host = siteConfig.restHost, port = siteConfig.restPort, i = 0, start = new Date().getTime();
            
            $http.get(url(host, port, vm.query))
                .success(function(data, status, headers, config) {
                    vm.resources = [];
                    data.forEach(function(obj) {
                        vm.resources.push(obj.resource.id);
                        vm.metrics[obj.resource.id] = [];
                        vm.selected[obj.resource.id] = [];
                        obj.metrics.forEach(function(metric) {
                            vm.metrics[obj.resource.id].push({'id': i++, 'label': metric});
                        });
                    });
                    vm.elapsed = (new Date().getTime() - start) / 1000;
                })
                .error(function(data, status, headers, config) {
                    alert("Search failed (status "+status+")");    // FIXME: do better
                    console.log(data, status, headers, config);
                });

            // Reset so that <input/> reverts to placeholder
            vm.query = '';
        }
    }

})();

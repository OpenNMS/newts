
(function() {
    'use strict';

    angular
        .module('newts.graphBuilder', [ 'newts.graphService', 'newts.siteConfig' ])
        .controller('GraphBuilderController', builder)
        .directive('graphBuilder', function() {
            return {
                restrict: 'E',
                scope: {},
                templateUrl: 'graph-builder.html',
                controller: 'GraphBuilderController',
                controllerAs: 'vm'                
            };
        });    

    // Transform from Newts JSON, to the Flot format.
    function transform(data, filter) {
        var transformed = [], i, j, column, labels = {}, len;

        for (i=0; i < data.length; i++) {
            for (j=0; j < data[i].length; j++) {
                column = data[i][j];

                if (filter.indexOf(column.name) >= 0) {
                    if (!labels.hasOwnProperty(column.name)) {
                        len = transformed.push({"label": column.name, "data": []});
                        labels[column.name] = len - 1;
                    }

                    transformed[labels[column.name]].data.push([
                        column.timestamp,
                        column.value
                    ]);
                }
            }
        }

        return transformed;
    }

    // Construct a measurements URL.
    function url(host, port, resource, resolution, range) {
        var urlString = 'http://'+host+':'+port+'/measurements/'+resource, args = [];

        if (resolution)  args.push('resolution='+resolution);
        if (range.start) args.push('start='+new Date(range.start).toISOString());
        if (range.end)   args.push('end='+new Date(range.end).toISOString());

        if (args.length > 0) {
            urlString += '?' + args.join('&');
        }

        return urlString;
    }

    /* format a localStorage persistence key */
    function keyFor(resource, property) {
        return resource + ':' + property;
    }

    // return a builder form representation, from storage if one is persisted, or
    // a newly initialized one otherwise.
    function initForm(resource, metrics) {
        var configForm = {
            '_interval':   '',
            '_resolution': '',
            '_range': {
                'start': null,
                'end':   null
            }
        };

        metrics.forEach(function(metric) {
            configForm[metric] = { 'function': 'AVERAGE' };
        });

        if (!Modernizr.localstorage) {
            console.log('warning: cannot retrieve state; browser does not support html local storage');
            return configForm;
        }

        configForm._interval = retrieveItem(resource, '_interval');
        configForm._resolution = retrieveItem(resource, '_resolution');
        configForm._range.start = retrieveItem(resource, '_range.start');
        configForm._range.end = retrieveItem(resource, '_range.end');

        metrics.forEach(function(metric) {
            configForm[metric] = {
                'label': retrieveItem(resource, metric + '.label'),
                'source': retrieveItem(resource, metric + '.source'),
                'function': retrieveItem(resource, metric + '.function', 'AVERAGE'),
                'heartbeat': retrieveItem(resource, metric + '.heartbeat')    
            };
            
        });

        return configForm;
    }

    /* serialize a builder form to localStorage */
    function persistForm(resource, formObj, metrics) {

        persistItem(keyFor(resource, '_interval'), formObj._interval);
        persistItem(keyFor(resource, '_resolution'), formObj._resolution);
        persistItem(keyFor(resource, '_range.start'), formObj._range.start);
        persistItem(keyFor(resource, '_range.end'), formObj._range.end);

        metrics.forEach(function(metric) {
            persistItem(keyFor(resource, metric + '.label'), formObj[metric].label);
            persistItem(keyFor(resource, metric + '.source'), metric);
            persistItem(keyFor(resource, metric + '.function'), formObj[metric].function);
            persistItem(keyFor(resource, metric + '.heartbeat'), formObj[metric].heartbeat); 
        });
    }

    /* store a value to localStorage */
    function persistItem(key, value) {
        if (!Modernizr.localstorage) {
            console.log('warning: cannot store state; browser does not support html local storage');
            return;
        }

        if (value) {
            window.localStorage[key] = value;
        }
        else {
            window.localStorage[key] = '';
        }
    }

    /* retrieve a value from localStorage */
    function retrieveItem(resource, key, fallback) {
        var value = window.localStorage[keyFor(resource, key)];
        return (value === undefined) ? fallback : value;
    }

    function builder($scope, $http, graphService, siteConfig) {
        /* jshint validthis: true */
        var vm = this, end, start;
        
        vm.showGraph = false;
        vm.availableFunctions = [ 'MIN', 'AVERAGE', 'MAX' ];
        vm.selectedMetrics = [];
        
        vm.getSelectedFunction = getSelectedFunction;
        vm.setSelectedFunction = setSelectedFunction;
        vm.refresh = drawGraph;
        
        // Bind to graph-selection signal.
        $scope.$on('graphServiceBroadcast', function() {
            vm.showGraph = true;

            vm.resource = graphService.message.id;
            vm.selectedMetrics = graphService.message.metrics;

            vm.configForm = initForm(vm.resource, vm.selectedMetrics);
            drawGraph();
        });

        function setSelectedFunction(metric, func) {
            vm.configForm[metric].function = func;
        }

        function getSelectedFunction(metric) {
            return vm.configForm[metric].function
        }
        
        function drawGraph() {
            var host, port, flotData, resultDescriptor, options;
            
            host = siteConfig.restHost;
            port = siteConfig.restPort;

            resultDescriptor = { 'interval': vm.configForm._interval, 'datasources': [], 'exports': [] };

            vm.selectedMetrics.forEach(function(metric) {
                resultDescriptor.datasources.push({
                    'label': vm.configForm[metric].label,
                    'source': metric,
                    'function': vm.configForm[metric].function,
                    'heartbeat': vm.configForm[metric].heartbeat
                });
                resultDescriptor.exports.push(vm.configForm[metric].label);
            });

            options = {
                series: {
                    points : { show: true },
                    lines  : { show: true }
                },
                xaxis: {
                    mode: "time",
                    timezone: "browser"
                },
                grid: {
                    hoverable: true,
                    clickable: false
                }
            };

            $http.post(url(host, port, vm.resource, vm.configForm._resolution, vm.configForm._range), resultDescriptor)
                .success(function(data, status, headers, config) {
                    flotData = transform(data, resultDescriptor.exports);
                    $.plot("#graph", flotData, options);
                })
                .error(function(data, status, headers, config) {
                    // What we really need is proper form validation, but in the meantime, we'll
                    // cheat by catching 400s (Bad Request), and force-displaying the builder form.
                    if (status === 400) {
                        // XXX: I assume there is a more angularjs-idiomatic way of doing this...
                        $('#graphConfigListGroup').addClass('in');    // Show the form
                        $('#graph').empty();    // Remove any existing graph
                    }
                    else {
                        alert("Failed (status "+status+")");
                        console.log(data, status, headers, config);
                    }
                });

            persistForm(vm.resource, vm.configForm, vm.selectedMetrics);
        }
    }
    builder.$inject = [ '$scope', '$http', 'graphService', 'siteConfig' ];


})();

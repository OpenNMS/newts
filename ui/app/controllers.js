

(function() {
    'use strict';

    angular
        .module('newts.controllers', [])
        .controller('MainController', mainController)
        .controller('DebugController', debugController);

    function mainController($scope) {
        // Dismiss the modal window when graphable values have been selected
        $scope.onSelect = function() {
            $('#searchModal').modal('toggle');
        };
    }

    mainController.$inject = [ '$scope' ];

    function debugController($scope, $http, $timeout) {
        /* jshint validthis: true */
        var vm = this,
            previous = 50,
            x,
            y,
            numSamples = 43200,    // 30 days
            interval = 60000,
            batchSize = 100,
            batch,
            resource,
            batchesComplete,
            httpPromise,
            url = 'http://localhost:8080/samples',
            now,
            start;

        vm.generateTestData = function() {
            updateProgress(0);
            $('#generateButton').attr('disabled', true);
            $('#generateButton').html('Generating...');
            $('#debugModal .progress').addClass('active');
            $timeout(generateTestData, 500);
        };

        function generateTestData() {
            now = new Date();
            start = new Date(now.getTime() - (numSamples*interval));
            x = start;
            batch = [];
            batchesComplete = 0;
            resource = rId();

            for (var i=0; i < numSamples; i++) {
                y = previous + Math.random() * 10 - 5;

                batch.push(sample(resource, x.getTime(), y));
                
                if (batch.length >= batchSize) {
                    if (httpPromise === undefined) {
                        httpPromise = $http.post(url, batch);
                    }
                     else {
                         (function(batch, batchesComplete) {
                             httpPromise = httpPromise.then(
                                 function(response) {    // success
                                     updateProgress(((batchesComplete * batchSize) * 100) / numSamples);
                                     return $http.post(url, batch);
                                 },
                                 function(response) {    // failure
                                     console.log('error writing samples, (status '+response.status+')');
                                 });
                         })(batch, batchesComplete);
                    }

                    batch = [];
                    batchesComplete++;
                }

                x = new Date(x.getTime() + interval);    // one minute later...
            }

            httpPromise.then(function() {
                $http.post(url, batch).then(
                    function(response) {    // success
                        updateProgress(100);
                        $('#generateButton').attr('disabled', false);
                        $('#generateButton').html('Generate Data');
                        $('#debugModal .progress').removeClass('active');
                        appendAlert(
                            '<p>Added '+numSamples+' samples from '+moment(start).format('YYYY-MM-DD hh:mm:ss')+' to '+moment(now).format('YYYY-MM-DD hh:mm:ss')+' at an interval of '+moment.duration(interval).humanize()+', for resource "'+resource+'"</p><p><i>Hint: also try searching "type:generated", or just "generated".</i></p>'
                        );
                    },
                    function(response) {   // failure
                        alert("Failed (status "+response.status+")");
                        console.log('error writing samples, (status '+response.status+')');
                    });
            });
        }

        function sample(id, timestamp, value) {
            return {
                resource: { id: id, attributes: { type: 'generated' }},
                timestamp: timestamp,
                name: 'data',
                type: 'GAUGE',
                value: value
            };
        }

        function rId() {
            function rand() {
                return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
            }
            return 'TestData_(' + rand() + rand() + ')';
        }

        function updateProgress(percentage) {
            $('#debugModal .progress-bar').width(percentage + '%');
        }

        function appendAlert(message) {
            $('#debugAlerts')
                .append('<div class="alert alert-success" role="alert">'+message+'</div>');
        }
    }

    debugController.$inject = [ '$scope', '$http', '$timeout' ];

})();

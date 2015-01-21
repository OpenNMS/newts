
(function() {
    'use strict';

    angular
        .module('newts.graphService', [])
        .factory('graphService', graphService);

    graphService.$inject = [ '$rootScope' ];

    function graphService($rootScope) {
        var service = {}; 
        service.message = '';

        service.broadcast = function (msg) {
            this.message = msg;
            $rootScope.$broadcast('graphServiceBroadcast');
        };

        return service;
    }

})();

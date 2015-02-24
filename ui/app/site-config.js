

(function() {
    'use strict';

    angular
        .module('newts.siteConfig', [])
        .service('siteConfig', function($location) {
            // Default to the same host that is serving the UI app
            this.restUrl = $location.protocol()+'://'+$location.host()+':'+$location.port();

            // Override the URL if one is assigned in localstorage
            if (window.localStorage.hasOwnProperty('_restUrl')) {
                var url = window.localStorage._restUrl;
                if (url) {
                    this.restUrl = url;
                }
            }

        });
    
})();

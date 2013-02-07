"use strict";

requirejs.config({
    baseUrl: 'assets/javascripts/lib'
});

requirejs(
    ['humane.min', 'jquery.min', 'angular.min'],
    function (humane) {
    }
);
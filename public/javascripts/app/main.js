(function ($, humane, angular) {
    "use strict";

    var axel = {};

    eventHandler($, humane, axel);

    axel.ws = new WebSocket("ws://localhost:9000/ws");

    axel.startNewDownload = function (url) {
        humane.log("Starting a new download...");

        axel.ws.send(JSON.stringify({ kind: 'newDownload', data: url }));
    };

    axel.ws.onerror = function (error) {
        humane.error(error);
    };

    axel.ws.onmessage = function (message) {
    };
})($, humane, angular);

function eventHandler($, humane, axel) {
    $("document").ready(function () {
        $("#url").parent().submit(function (event) {
            var urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
            var $url = $("#url");

            if (!urlRegex.test($url.val()))
                alert("Please enter a valid URL");

            axel.startNewDownload($url.val());

            $url.val("");

            event.preventDefault();
        });
    });
}


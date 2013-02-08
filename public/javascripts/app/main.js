(function ($, humane, angular) {
    "use strict";

    var axel = {};

    eventHandler($, humane, axel);

    axel.ws = new WebSocket("ws://localhost:9000/ws");

    axel.startNewDownload = function (url) {
        axel.ws.send(JSON.stringify({ kind: 'newDownload', data: url }));
    };

    axel.ws.onerror = function (error) {
        humane.error(error);
    };

    axel.ws.onmessage = function (message) {
        message = JSON.parse(message.data)

        switch (message.kind) {
            case 'info':
                humane.log(message.data)
                break;
            case 'error':
                humane.log(message.data)
                break;
            default:
                break;
        }
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


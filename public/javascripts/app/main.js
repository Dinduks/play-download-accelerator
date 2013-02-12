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
            case 'downloadFinished':
                humane.log(message.data)
                break;
            default:
                break;
        }
    };
})($, humane, angular);

function eventHandler($, humane, axel) {
    $("document").ready(function () {
        $("#path").parent().keypress(function (event) {
            if (13 != event.which) return;

            var errorMessage = "";

            var urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
            var $url = $("#url");
            var $path = $("#path");

            if (!urlRegex.test($url.val())) errorMessage += "Please enter a valid URL<br />";
            if ("" === $path.val())         errorMessage += "Please enter a path";

            if ("" !== errorMessage) {
                humane.log(errorMessage)
            } else {
                axel.startNewDownload($url.val());
                $url.val("");
            }

            event.preventDefault();
        });
    });
}


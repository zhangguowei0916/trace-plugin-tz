/**
 * cordova is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 */


var exec = require("cordova/exec");

/**
 * Constructor.
 *
 * @returns {BarcodeScanner}
 */
function TracePluginCommon() {
};

/**
 * Read code from scanner.
 *
 * @param {Function} successCallback This function will recieve a result object: {
 *        text : '12345-mock',    // The code that was scanned.
 *        format : 'FORMAT_NAME', // Code format.
 *        cancelled : true/false, // Was canceled.
 *    }
 * @param {Function} errorCallback
 */
TracePluginCommon.prototype.show =
 function(content){
    exec(
    function(message){//成功回调function
        console.log(message);
    },
    function(message){//失败回调function
        console.log(message);
    },
    "TracePluginCommon",//feature name
    "show",//action
    [content]//要传递的参数，json格式
    );
}


var tracePluginCommon = new TracePluginCommon();
module.exports = tracePluginCommon;

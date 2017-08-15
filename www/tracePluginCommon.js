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
TracePluginCommon.prototype.link =
 function(content){
    exec(
    function(message){//成功回调function
        console.log(message);
    },
    function(message){//失败回调function
        console.log(message);
    },
    "TracePluginCommon",//feature name
    "link",//action
    [content]//要传递的参数，json格式
    );
}

 TracePluginCommon.prototype.scan =
  function(successCallback, errorCallback, content){

    if (typeof errorCallback != "function") {
        console.log("BarcodeScanner.scan failure: failure parameter not a function");
        return;
    }

    if (typeof successCallback != "function") {
        console.log("BarcodeScanner.scan failure: success callback parameter must be a function");
        return;
    }

     exec(successCallback,errorCallback,
     "TracePluginCommon",//feature name
     "scan",//action
     [content]//要传递的参数，json格式
     );
 }
TracePluginCommon.prototype.print =
 function(content){
 console.log(content);
    exec(
    function(message){//成功回调function
        console.log(message);
    },
    function(message){//失败回调function
        console.log(message);
    },
    "TracePluginCommon",//feature name
    "print",//action
    [content]//要传递的参数，json格式
    );
}


TracePluginCommon.prototype.update =
 function(content){
 console.log(content);
    exec(
    function(message){//成功回调function
        console.log(message);
    },
    function(message){//失败回调function
        console.log(message);
    },
    "TracePluginCommon",//feature name
    "update",//action
    [content]//要传递的参数，json格式
    );
}


TracePluginCommon.prototype.setting =
 function(content){
 console.log(content);
    exec(
    function(message){//成功回调function
        console.log(message);
    },
    function(message){//失败回调function
        console.log(message);
    },
    "TracePluginCommon",//feature name
    "setting",//action
    [content]//要传递的参数，json格式
    );
}


var tracePluginCommon = new TracePluginCommon();
module.exports = tracePluginCommon;

var reportedPositionFailures = 0;
navigator.geolocation.getCurrentPosition = function(success, failure, options) { //override because default implementation requires GPS permission
    geoposition = JSON.parse(GBjs.getCurrentPosition());

    if(options && options.maximumAge && (geoposition.timestamp < Date.now() - options.maximumAge) && reportedPositionFailures <= 10 ) {
        reportedPositionFailures++;
    	failure({ code: 2, message: "POSITION_UNAVAILABLE"});
    } else {
        reportedPositionFailures = 0;
        success(geoposition);
    }
};

if (window.Storage){
    var prefix = GBjs.getAppLocalstoragePrefix();
    GBjs.gbLog("redefining local storage with prefix: " + prefix);

    Storage.prototype.setItem = (function(key, value) {
        this.call(localStorage,prefix + key, value);
    }).bind(Storage.prototype.setItem);

    Storage.prototype.getItem = (function(key) {
//        console.log("I am about to return " + prefix + key);
        var def = null;
        if(key == 'clay-settings') {
            def = '{}';
        }
        return this.call(localStorage,prefix + key) || def;
    }).bind(Storage.prototype.getItem);
}

function loadScript(url, callback) {
    // Adding the script tag to the head as suggested before
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = url;

    // Then bind the event to the callback function.
    // There are several events for cross browser compatibility.
    script.onreadystatechange = callback;
    script.onload = callback;

    // Fire the loading
    head.appendChild(script);
}

function getURLVariable(variable, defaultValue) {
  // Find all URL parameters
  var query = location.search.substring(1);
  var vars = query.split('&');
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split('=');

    // If the query variable parameter is found, decode it to use and return it for use
    if (pair[0] === variable) {
      return decodeURIComponent(pair[1]);
    }
  }
  return defaultValue || false;
}

function showStep(desiredStep) {
    var steps = document.getElementsByClassName("step");
    var testStep = null;
    for (var i = 0; i < steps.length; i ++) {
        if (steps[i].id == desiredStep)
            testStep = steps[i].id;
    }
    if (testStep !== null) {
        for (var i = 0; i < steps.length; i ++) {
            steps[i].style.display = 'none';
        }
        document.getElementById(desiredStep).style.display="block";
    }
}

function hideSteps() {
    var steps = document.getElementsByClassName("step");
        for (var i = 0; i < steps.length; i ++) {
            steps[i].style.display = 'none';
        }
}

function gbPebble() {
    this.configurationURL = null;
    this.configurationValues = null;

    this.appMessageCallbackACK = {};
    this.appMessageCallbackNACK = {};

    var self = this;

    self.appMessageCallbackProcessed = function (transactionId) {
        if (self.appMessageCallbackACK[transactionId]) {
            self.removeEventListener("ACK"+transactionId, self.appMessageCallbackACK[transactionId]);
            self.appMessageCallbackACK[transactionId] = undefined;
        }
        if (self.appMessageCallbackNACK[transactionId]) {
            self.removeEventListener("NACK"+transactionId, self.appMessageCallbackNACK[transactionId]);
            self.appMessageCallbackNACK[transactionId] = undefined;
        }
    }
    self.events = {};
    //events processing: see http://stackoverflow.com/questions/10978311/implementing-events-in-my-own-object
    self.addEventListener = function(name, handler) {
        if (self.events.hasOwnProperty(name))
            self.events[name].push(handler);
        else
            self.events[name] = [handler];
    }

    self.removeEventListener = function(name, handler) {
        if (!self.events.hasOwnProperty(name))
            return;

        var index = self.events[name].indexOf(handler);
        if (index != -1)
            self.events[name].splice(index, 1);
    }

    self.evaluate = function(name, args) {
        if (!self.events.hasOwnProperty(name))
            return;

        if (!args || !args.length)
            args = [];

        var evs = self.events[name], l = evs.length;
        for (var i = 0; i < l; i++) {
            evs[i].apply(null, args);
        }
        GBjs.eventFinished(name);
    }

    this.actuallyOpenURL = function() {
        showStep("step1compat");
        window.open(self.configurationURL.toString(), "config");
    }

    this.sendConfiguration = function() {
        GBjs.sendAppMessage(document.getElementById("jsondata").innerHTML, false );
        showStep("step1");
        GBActivity.closeActivity();
    }

    this.savePreset = function() {
        GBjs.saveAppStoredPreset(self.configurationValues);
    }

    this.loadPreset = function() {
        showStep("step2");
        var presetElements = document.getElementsByClassName("store_presets");
        for (var i = 0; i < presetElements.length; i ++) {
            presetElements[i].style.display = 'none';
        }
        self.configurationValues = GBjs.getAppStoredPreset();
        document.getElementById("jsondata").innerHTML=self.configurationValues;
    }

    //needs to be called like this because of original Pebble function name
    this.openURL = function(url) {
            if (url.lastIndexOf("http", 0) === 0) {
                    document.getElementById("config_url").innerHTML=url;
                    var UUID = GBjs.getAppUUID();
                    self.configurationURL = new Uri(url).addQueryParam("return_to", "gadgetbridge://"+UUID+"?config=true&json=");
            } else {
                //TODO: add custom return_to
                var iframe = document.getElementsByTagName('iframe')[0];
                var oldbody = document.getElementsByTagName("body")[0];
                if (iframe === undefined && oldbody !== undefined) {
                    iframe = document.createElement("iframe");
                    oldbody.parentNode.replaceChild(iframe,oldbody);
                } else {
                    hideSteps();
                    document.documentElement.appendChild(iframe);
                }

                iframe.src = url;

            }

    }

    this.getActiveWatchInfo = function() {
        return JSON.parse(GBjs.getActiveWatchInfo());
    }

    this.sendAppMessage = function (dict, callbackAck, callbackNack){
        try {
            self.configurationValues = JSON.stringify(dict);
            if (document.getElementById("step2").style.display == 'block' && document.getElementById("jsondata").innerHTML == "") { //intercept the values
                document.getElementById("jsondata").innerHTML=self.configurationValues;
            } else { //pass them silently
                var needsTransaction = false;
                if (callbackAck != undefined || callbackNack != undefined) {
                    needsTransaction = true;
                }
                var transactionId = GBjs.sendAppMessage(JSON.stringify(dict), needsTransaction);
                if (needsTransaction) {
                    if (typeof callbackAck != "undefined") {
                        self.appMessageCallbackACK[transactionId] = function(e) {
//                            console.log("ACK FOR " + JSON.stringify(e));
                            callbackAck(e);
                            self.appMessageCallbackProcessed(transactionId);
                        };

                        this.addEventListener("ACK"+transactionId, self.appMessageCallbackACK[transactionId]);

                    }
                    if (typeof callbackNack != "undefined") {
                        self.appMessageCallbackNACK[transactionId] = function(e) {
//                            console.log("NACK FOR " + JSON.stringify(e));
                            callbackNack(e);
                            self.appMessageCallbackProcessed(transactionId);
                        };

                        this.addEventListener("NACK"+transactionId, self.appMessageCallbackNACK[transactionId]);
                    }
                }
            }
        }
        catch (e) {
            GBjs.gbLog("sendAppMessage failed: " + e);
        }
    }

    this.getAccountToken = function() {
        return '';
    }

    this.getWatchToken = function() {
        return GBjs.getWatchToken();
    }

    this.getTimelineToken = function() {
        return '';
    }

    this.showSimpleNotificationOnPebble = function(title, body) {
        GBjs.gbLog("app wanted to show: " + title + " body: "+ body);
    }


    this.showConfiguration = function() {
        console.error("This watchapp doesn't support configuration");
        GBActivity.closeActivity();
    }

    this.parseReturnedPebbleJS = function() {
        var str = document.getElementById('pastereturn').value;
        var needle = "pebblejs://close#";

        if (str.split(needle)[1] !== undefined) {
            var t = new Object();
            t.response = decodeURIComponent(str.split(needle)[1]);
            showStep("step2");
            self.evaluate('webviewclosed',[t]);
        } else {
            console.error("No valid configuration found in the entered string.");
        }
    }
}

var Pebble = new gbPebble();

var jsConfigFile = GBjs.getAppConfigurationFile();
var storedPreset = GBjs.getAppStoredPreset();

document.addEventListener('DOMContentLoaded', function(){
if (jsConfigFile != null) {
    loadScript(jsConfigFile, function() {
        Pebble.evaluate('ready', [{'type': "ready"}]); //callback object apparently needed by some watchfaces
        if(document.hasFocus() && !(getURLVariable('config') == 'true')) {
            Pebble.evaluate('showConfiguration');
        } else {
            window.onfocus = function () {
                showStep("step1");
                GBjs.gbLog("window focused!!!");
                Pebble.evaluate('showConfiguration');
            };
        }
        if (getURLVariable('config') == 'true') {
            showStep("step2");
            var json_string = getURLVariable('json');
            var t = new Object();
            t.response = json_string;
            if (json_string != '') {
                Pebble.evaluate('webviewclosed',[t]);
            }
        } else {
            if (storedPreset === undefined) {
                var presetElements = document.getElementsByClassName("load_presets");
                    for (var i = 0; i < presetElements.length; i ++) {
                        presetElements[i].style.display = 'none';
                    }
            }
        }
    });
}
}, false);
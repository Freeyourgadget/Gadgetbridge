if (window.Storage){
    var prefix = GBjs.getAppUUID();
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

function gbPebble() {
    this.configurationURL = null;
    this.configurationValues = null;
    var self = this;

    this.addEventListener = function(e, f) {
        if(e == 'ready') {
            self.ready = f;
        }
        if(e == 'showConfiguration') {
            self.showConfiguration = f;
        }
        if(e == 'webviewclosed') {
            self.parseconfig = f;
        }
        if(e == 'appmessage') {
            self.appmessage = f;
        }
    }

    this.removeEventListener = function(e, f) {
        if(e == 'ready') {
            self.ready = null;
        }
        if(e == 'showConfiguration') {
            self.showConfiguration = null;
        }
        if(e == 'webviewclosed') {
            self.parseconfig = null;
        }
        if(e == 'appmessage') {
            self.appmessage = null;
        }
    }
    this.actuallyOpenURL = function() {
        showStep("step1compat");
        window.open(self.configurationURL.toString(), "config");
    }

    this.actuallySendData = function() {
        GBjs.sendAppMessage(self.configurationValues);
        GBjs.closeActivity();
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
                location.href = url;
            }

    }

    this.getActiveWatchInfo = function() {
        return JSON.parse(GBjs.getActiveWatchInfo());
    }

    this.sendAppMessage = function (dict, callbackAck, callbackNack){
        try {
            self.configurationValues = JSON.stringify(dict);
            document.getElementById("jsondata").innerHTML=self.configurationValues;
            return callbackAck;
        }
        catch (e) {
            GBjs.gbLog("sendAppMessage failed");
            return callbackNack;
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

    this.ready = function() {
    }

    this.showConfiguration = function() {
        console.error("This watchapp doesn't support configuration");
        GBjs.closeActivity();
    }

    this.parseReturnedPebbleJS = function() {
        var str = document.getElementById('pastereturn').value;
        var needle = "pebblejs://close#";

        if (str.split(needle)[1] !== undefined) {
            var t = new Object();
            t.response = unescape(str.split(needle)[1]);
            self.parseconfig(t);
            showStep("step2");
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
        if (getURLVariable('config') == 'true') {
            showStep("step2");
            var json_string = unescape(getURLVariable('json'));
            var t = new Object();
            t.response = json_string;
            if (json_string != '')
                Pebble.parseconfig(t);
        } else {
            if (storedPreset === undefined) {
                var presetElements = document.getElementsByClassName("load_presets");
                    for (var i = 0; i < presetElements.length; i ++) {
                        presetElements[i].style.display = 'none';
                    }
            }
            Pebble.ready();
            Pebble.showConfiguration();
        }
    });
}
}, false);
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

function gbPebble() {
    this.configurationURL = null;
    this.configurationValues = null;

    this.addEventListener = function(e, f) {
        if(e == 'ready') {
            this.ready = f;
        }
        if(e == 'showConfiguration') {
            this.showConfiguration = f;
        }
        if(e == 'webviewclosed') {
            this.parseconfig = f;
        }
        if(e == 'appmessage') {
            this.appmessage = f;
        }
    }

    this.removeEventListener = function(e, f) {
        if(e == 'ready') {
            this.ready = null;
        }
        if(e == 'showConfiguration') {
            this.showConfiguration = null;
        }
        if(e == 'webviewclosed') {
            this.parseconfig = null;
        }
        if(e == 'appmessage') {
            this.appmessage = null;
        }
    }
    this.actuallyOpenURL = function() {
        window.open(this.configurationURL.toString(), "config");
    }

    this.actuallySendData = function() {
        GBjs.sendAppMessage(this.configurationValues);
    }

    //needs to be called like this because of original Pebble function name
    this.openURL = function(url) {
        document.getElementById("config_url").innerHTML=url;
        var UUID = GBjs.getAppUUID();
        this.configurationURL = new Uri(url).addQueryParam("return_to", "gadgetbridge://"+UUID+"?config=true&json=");
    }

    this.getActiveWatchInfo = function() {
        return JSON.parse(GBjs.getActiveWatchInfo());
    }

    this.sendAppMessage = function (dict, callbackAck, callbackNack){
        try {
            this.configurationValues = JSON.stringify(dict);
            document.getElementById("jsondata").innerHTML=this.configurationValues;
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

    this.showSimpleNotificationOnPebble = function(title, body) {
        GBjs.gbLog("app wanted to show: " + title + " body: "+ body);
    }


}

var Pebble = new gbPebble();

var jsConfigFile = GBjs.getAppConfigurationFile();
if (jsConfigFile != null) {
    loadScript(jsConfigFile, function() {
        if (getURLVariable('config') == 'true') {
            document.getElementById('step1').style.display="none";
            var json_string = unescape(getURLVariable('json'));
            var t = new Object();
            t.response = json_string;
            if (json_string != '')
                Pebble.parseconfig(t);
        } else {
            document.getElementById('step2').style.display="none";
            Pebble.showConfiguration();
        }
    });
}

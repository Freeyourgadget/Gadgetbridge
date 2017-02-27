package nodomain.freeyourgadget.gadgetbridge.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

public class WebViewSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(WebViewSingleton.class);

    private static WebView instance = null;
    private static JSInterface jsInterface;
    private static MutableContextWrapper contextWrapper = null;

    private WebViewSingleton() {
    }

    public static synchronized WebView createWebView(Context context) {
        if (instance == null) {
            contextWrapper = new MutableContextWrapper(context);
            instance = new WebView(contextWrapper);
            instance.setWillNotDraw(true);
            instance.clearCache(true);
            instance.setWebViewClient(new GBWebClient());
            instance.setWebChromeClient(new GBChromeClient());
            instance.setWebContentsDebuggingEnabled(true);
            WebSettings webSettings = instance.getSettings();
            webSettings.setJavaScriptEnabled(true);
            //needed to access the DOM
            webSettings.setDomStorageEnabled(true);
            //needed for localstorage
            webSettings.setDatabaseEnabled(true);
        }
        return instance;
    }

    public static void updateActivityContext(Context context) {
        if (context instanceof Activity) {
            contextWrapper.setBaseContext(context);
        }
    }

    @NonNull
    public static WebView getWebView() {
        return instance;
    }

    public static void runJavascriptInterface(final GBDevice device, final UUID uuid) {
        if (jsInterface == null || !device.equals(jsInterface.device) || !uuid.equals(jsInterface.mUuid)) {
            jsInterface = new JSInterface(device, uuid);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    instance.removeJavascriptInterface("GBjs");
                    instance.addJavascriptInterface(jsInterface, "GBjs");
                    instance.loadUrl("file:///android_asset/app_config/configure.html");
                }
            });
        } else {
            LOG.debug("Not replacing the JS in the webview. JS uuid " + jsInterface.mUuid.toString());
        }
    }

    public static void appMessage(String message) {

        if (instance == null)
            return;

        final String appMessage = jsInterface.parseIncomingAppMessage(message);
        LOG.debug("to WEBVIEW: " + appMessage);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    instance.evaluateJavascript("Pebble.evaluate('appmessage',[" + appMessage + "]);", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            //TODO: the message should be acked here instead of in PebbleIoThread
                            LOG.debug("Callback from appmessage", s);
                        }
                    });
                } else {
                    instance.loadUrl("javascript:Pebble.evaluate('appmessage',[" + appMessage + "]);");
                }
            }
        });
    }

    public static void disposeWebView() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (instance != null) {
                    instance.setWebChromeClient(null);
                    instance.setWebViewClient(null);
                    instance.clearHistory();
                    instance.clearCache(true);
                    instance.loadUrl("about:blank");
                    instance.freeMemory();
                    instance.pauseTimers();
//                    instance.destroy();
//                    instance = null;
//                    contextWrapper = null;
                    jsInterface = null;
                }
            }
        });
    }

    private static class CurrentPosition {
        long timestamp;
        double altitude;
        float latitude, longitude, accuracy, speed;

        public CurrentPosition() {
            Prefs prefs = GBApplication.getPrefs();
            this.latitude = prefs.getFloat("location_latitude", 0);
            this.longitude = prefs.getFloat("location_longitude", 0);
            LOG.info("got longitude/latitude from preferences: " + latitude + "/" + longitude);

            this.timestamp = (System.currentTimeMillis() / 1000) - 86400; //let accessor know this value is really old

            if (ActivityCompat.checkSelfPermission(contextWrapper, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    prefs.getBoolean("use_updated_location_if_available", false)) {
                LocationManager locationManager = (LocationManager) contextWrapper.getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria, false);
                if (provider != null) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                    if (lastKnownLocation != null) {
                        this.timestamp = lastKnownLocation.getTime();

                        this.latitude = (float) lastKnownLocation.getLatitude();
                        this.longitude = (float) lastKnownLocation.getLongitude();
                        this.accuracy = (float) lastKnownLocation.getAccuracy();
                        this.altitude = (float) lastKnownLocation.getAltitude();
                        this.speed = (float) lastKnownLocation.getSpeed();

                    }
                }
            }


        }
    }


    private static WebResourceResponse mimicOpenWeatherMapResponse() {
        WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();

        if (weatherSpec == null) return null;
        //location block

        CurrentPosition currentPosition = new CurrentPosition();
        GregorianCalendar[] sunrise = SPA.calculateSunriseTransitSet(new GregorianCalendar(), currentPosition.latitude, currentPosition.longitude, DeltaT.estimate(new GregorianCalendar()));

        JSONObject resp = new JSONObject();
        JSONObject coord = new JSONObject();
        JSONObject sys = new JSONObject();
        JSONArray weather = new JSONArray();
        JSONObject currCond = new JSONObject();
        JSONObject main = new JSONObject();
        try {
            coord.put("lat", currentPosition.latitude);
            coord.put("lon", currentPosition.longitude);

            sys.put("country", "World");
            sys.put("sunrise", (sunrise[0].getTimeInMillis() / 1000));
            sys.put("sunset", (sunrise[2].getTimeInMillis() / 1000));

            currCond.put("id", weatherSpec.currentConditionCode);
            currCond.put("main", weatherSpec.currentCondition);
            currCond.put("icon", Weather.mapToOpenWeatherMapIcon(weatherSpec.currentConditionCode));
            weather.put(currCond);

            main.put("temp", weatherSpec.currentTemp);
            main.put("temp_min", weatherSpec.todayMinTemp);
            main.put("temp_max", weatherSpec.todayMaxTemp);
            main.put("name", weatherSpec.location);

            resp.put("coord", coord);
            resp.put("sys", sys);
            resp.put("weather", weather);
            resp.put("main", main);
            LOG.info("WEBVIEW - mimic openweather response" + resp.toString());
            HashMap headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return new WebResourceResponse("application/json", "utf-8", 200, "OK",
                        headers,
                        new ByteArrayInputStream(resp.toString().getBytes())
                );
            } else {
                return new WebResourceResponse("application/json", "utf-8", new ByteArrayInputStream(resp.toString().getBytes()));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;

    }

    private static class GBChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (ConsoleMessage.MessageLevel.ERROR.equals(consoleMessage.messageLevel())) {
                GB.toast(formatConsoleMessage(consoleMessage), Toast.LENGTH_LONG, GB.ERROR);
                //TODO: show error page
            }
            return super.onConsoleMessage(consoleMessage);
        }

    }

    private static String formatConsoleMessage(ConsoleMessage message) {
        String sourceId = message.sourceId();
        if (sourceId == null || sourceId.length() == 0) {
            sourceId = "unknown";
        }
        return String.format("%s (at %s: %d)", message.message(), sourceId, message.lineNumber());
    }

    private static class GBWebClient extends WebViewClient {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            LOG.debug("WEBVIEW shouldInterceptRequest URL" + request.getUrl());
            if (request.getUrl().toString().startsWith("http://api.openweathermap.org") || request.getUrl().toString().startsWith("https://api.openweathermap.org")) {
                return mimicOpenWeatherMapResponse();
            } else {
                LOG.debug("WEBVIEW request:" + request.getUrl().toString() + " denied");
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            LOG.debug("WEBVIEW shouldInterceptRequest URL" + url);
            if (url.startsWith("http://api.openweathermap.org") || url.startsWith("https://api.openweathermap.org")) {
                return mimicOpenWeatherMapResponse();
            } else {
                LOG.debug("WEBVIEW request:" + url + " denied");
            }
            return super.shouldInterceptRequest(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                contextWrapper.startActivity(i);
            } else {
                url = url.replaceFirst("^pebblejs://close#", "file:///android_asset/app_config/configure.html?config=true&json=");
                view.loadUrl(url);
            }

            return true;
        }
    }


    private static JSONObject getAppConfigurationKeys(UUID uuid) {
        try {
            File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
            File configurationFile = new File(destDir, uuid.toString() + ".json");
            if (configurationFile.exists()) {
                String jsonstring = FileUtils.getStringFromFile(configurationFile);
                JSONObject json = new JSONObject(jsonstring);
                return json.getJSONObject("appKeys");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class JSInterface {

        UUID mUuid;
        GBDevice device;

        public JSInterface(GBDevice device, UUID mUuid) {
            LOG.debug("Creating JS interface for UUID: " + mUuid.toString());
            this.device = device;
            this.mUuid = mUuid;
        }

        public String parseIncomingAppMessage(String msg) {
            JSONObject jsAppMessage = new JSONObject();

            JSONObject knownKeys = getAppConfigurationKeys(this.mUuid);
            HashMap<Integer, String> appKeysMap = new HashMap<Integer, String>();

            String inKey, outKey;
            //knownKeys contains "name"->"index", we need to reverse that
            for (Iterator<String> key = knownKeys.keys(); key.hasNext(); ) {
                inKey = key.next();
                appKeysMap.put(knownKeys.optInt(inKey), inKey);
            }

            try {
                JSONArray incoming = new JSONArray(msg);
                JSONObject outgoing = new JSONObject();
                for (int i = 0; i < incoming.length(); i++) {
                    JSONObject in = incoming.getJSONObject(i);
                    outKey = null;
                    Object outValue = null;
                    for (Iterator<String> key = in.keys(); key.hasNext(); ) {
                        inKey = key.next();
                        switch (inKey) {
                            case "key":
                                outKey = appKeysMap.get(in.optInt(inKey));
                                break;
                            case "value":
                                outValue = in.get(inKey);
                        }
                    }
                    if (outKey != null && outValue != null) {
                        outgoing.put(outKey, outValue);
                    }
                }
                jsAppMessage.put("payload", outgoing);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsAppMessage.toString();
        }


        private boolean isLocationEnabledForWatchApp() {
            return true; //as long as we don't give watchapp internet access it's not a problem
        }

        @JavascriptInterface
        public void gbLog(String msg) {
            LOG.debug("WEBVIEW", msg);
        }

        @JavascriptInterface
        public void sendAppMessage(String msg) {
            LOG.debug("from WEBVIEW: " + msg);
            JSONObject knownKeys = getAppConfigurationKeys(this.mUuid);

            try {
                JSONObject in = new JSONObject(msg);
                JSONObject out = new JSONObject();
                String inKey, outKey;
                boolean passKey;
                for (Iterator<String> key = in.keys(); key.hasNext(); ) {
                    passKey = false;
                    inKey = key.next();
                    outKey = null;
                    int pebbleAppIndex = knownKeys.optInt(inKey, -1);
                    if (pebbleAppIndex != -1) {
                        passKey = true;
                        outKey = String.valueOf(pebbleAppIndex);
                    } else {
                        //do not discard integer keys (see https://developer.pebble.com/guides/communication/using-pebblekit-js/ )
                        Scanner scanner = new Scanner(inKey);
                        if (scanner.hasNextInt() && inKey.equals("" + scanner.nextInt())) {
                            passKey = true;
                            outKey = inKey;
                        }
                    }

                    if (passKey) {
                        Object obj = in.get(inKey);
                        if (obj instanceof Boolean) {
                            obj = ((Boolean) obj) ? "true" : "false";
                        }
                        out.put(outKey, obj);
                    } else {
                        GB.toast("Discarded key " + inKey + ", not found in the local configuration and is not an integer key.", Toast.LENGTH_SHORT, GB.WARN);
                    }

                }
                LOG.info("WEBV:" + out.toString());
                GBApplication.deviceService().onAppConfiguration(this.mUuid, out.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String getActiveWatchInfo() {
            JSONObject wi = new JSONObject();
            try {
                wi.put("firmware", device.getFirmwareVersion());
                wi.put("platform", PebbleUtils.getPlatformName(device.getModel()));
                wi.put("model", PebbleUtils.getModel(device.getModel()));
                //TODO: use real info
                wi.put("language", "en");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Json not supported apparently, we need to cast back and forth
            return wi.toString();
        }

        @JavascriptInterface
        public String getAppConfigurationFile() {
            try {
                File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
                File configurationFile = new File(destDir, this.mUuid.toString() + "_config.js");
                if (configurationFile.exists()) {
                    return "file:///" + configurationFile.getAbsolutePath();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @JavascriptInterface
        public String getAppStoredPreset() {
            try {
                File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
                File configurationFile = new File(destDir, this.mUuid.toString() + "_preset.json");
                if (configurationFile.exists()) {
                    return FileUtils.getStringFromFile(configurationFile);
                }
            } catch (IOException e) {
                GB.toast("Error reading presets", Toast.LENGTH_LONG, GB.ERROR);
                e.printStackTrace();
            }
            return null;
        }

        @JavascriptInterface
        public void saveAppStoredPreset(String msg) {
            Writer writer;

            try {
                File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
                File presetsFile = new File(destDir, this.mUuid.toString() + "_preset.json");
                writer = new BufferedWriter(new FileWriter(presetsFile));
                writer.write(msg);
                writer.close();
                GB.toast("Presets stored", Toast.LENGTH_SHORT, GB.INFO);
            } catch (IOException e) {
                GB.toast("Error storing presets", Toast.LENGTH_LONG, GB.ERROR);
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String getAppUUID() {
            return this.mUuid.toString();
        }

        @JavascriptInterface
        public String getAppLocalstoragePrefix() {
            String prefix = device.getAddress() + this.mUuid.toString();
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] bytes = prefix.getBytes("UTF-8");
                digest.update(bytes, 0, bytes.length);
                bytes = digest.digest();
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < bytes.length; i++) {
                    sb.append(String.format("%02X", bytes[i]));
                }
                return sb.toString().toLowerCase();
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                e.printStackTrace();
                return prefix;
            }
        }

        @JavascriptInterface
        public String getWatchToken() {
            //specification says: A string that is guaranteed to be identical for each Pebble device for the same app across different mobile devices. The token is unique to your app and cannot be used to track Pebble devices across applications. see https://developer.pebble.com/docs/js/Pebble/
            return "gb" + this.mUuid.toString();
        }


        @JavascriptInterface
        public String getCurrentPosition() {
            if (!isLocationEnabledForWatchApp()) {
                return "";
            }
            //we need to override this because the coarse location is not enough for the android webview, we should add the permission for fine location.
            JSONObject geoPosition = new JSONObject();
            JSONObject coords = new JSONObject();
            try {

                CurrentPosition currentPosition = new CurrentPosition();

                geoPosition.put("timestamp", currentPosition.timestamp);

                coords.put("latitude", currentPosition.latitude);
                coords.put("longitude", currentPosition.longitude);
                coords.put("accuracy", currentPosition.accuracy);
                coords.put("altitude", currentPosition.altitude);
                coords.put("speed", currentPosition.speed);

                geoPosition.put("coords", coords);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            LOG.info("WEBVIEW - geo position" + geoPosition.toString());
            return geoPosition.toString();
        }

    }

}

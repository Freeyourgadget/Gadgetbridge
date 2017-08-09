package nodomain.freeyourgadget.gadgetbridge.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.SparseArray;
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
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppMessage;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;

public class WebViewSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(WebViewSingleton.class);

    private WebView instance = null;
    private MutableContextWrapper contextWrapper;
    private Looper mainLooper;
    private static WebViewSingleton webViewSingleton = new WebViewSingleton();
    private static UUID currentRunningUUID;
    private static Messenger internetHelper = null;
    private static boolean internetHelperBound;
    private static CountDownLatch latch;
    private static WebResourceResponse internetResponse;
    private final static Messenger internetHelperListener = new Messenger(new IncomingHandler());

    private WebViewSingleton() {
    }

    public static synchronized WebView getInstance(Activity context) {
        if (webViewSingleton.instance == null) {
            webViewSingleton.contextWrapper = new MutableContextWrapper(context);
            webViewSingleton.mainLooper = context.getMainLooper();
            webViewSingleton.instance = new WebView(webViewSingleton.contextWrapper);
            WebView.setWebContentsDebuggingEnabled(true);
            webViewSingleton.instance.setWillNotDraw(true);
            webViewSingleton.instance.clearCache(true);
            webViewSingleton.instance.setWebViewClient(new GBWebClient());
            webViewSingleton.instance.setWebChromeClient(new GBChromeClient());
            WebSettings webSettings = webViewSingleton.instance.getSettings();
            webSettings.setJavaScriptEnabled(true);
            //needed to access the DOM
            webSettings.setDomStorageEnabled(true);
            //needed for localstorage
            webSettings.setDatabaseEnabled(true);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("nodomain.freeyourgadget.internethelper", "nodomain.freeyourgadget.internethelper.MyService"));
            context.getApplicationContext().bindService(intent, internetHelperConnection, Context.BIND_AUTO_CREATE);
        }
        return webViewSingleton.instance;
    }

    //Internet helper outgoing connection
    private static ServiceConnection internetHelperConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LOG.info("internet helper service bound");
            internetHelperBound = true;
            internetHelper = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            LOG.info("internet helper service unbound");
            internetHelper = null;
            internetHelperBound = false;
        }
    };

    //Internet helper inbound (responses) handler
    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            LOG.debug("WEBVIEW: internet helper returned: " + data.getString("response"));
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                internetResponse = new WebResourceResponse(data.getString("content-type"), data.getString("content-encoding"), 200, "OK",
                        headers,
                        new ByteArrayInputStream(data.getString("response").getBytes())
                );
            } else {
                internetResponse = new WebResourceResponse(data.getString("content-type"), data.getString("content-encoding"), new ByteArrayInputStream(data.getString("response").getBytes()));
            }

            latch.countDown();
        }
    }

    public static void updateActivityContext(Activity context) {
        if (context != null) {
            webViewSingleton.contextWrapper.setBaseContext(context);
        }
    }

    @NonNull
    public static WebView getWebView() {
        return webViewSingleton.instance;
    }

    public static void runJavascriptInterface(GBDevice device, UUID uuid) {
        if (uuid.equals(currentRunningUUID)) {
            LOG.debug("WEBVIEW uuid not changed keeping the old context");
        } else {
            final JSInterface jsInterface = new JSInterface(device, uuid);
            LOG.debug("WEBVIEW uuid changed, restarting");
            currentRunningUUID = uuid;
            new Handler(webViewSingleton.mainLooper).post(new Runnable() {
                @Override
                public void run() {
                    webViewSingleton.instance.removeJavascriptInterface("GBjs");
                    webViewSingleton.instance.addJavascriptInterface(jsInterface, "GBjs");
                    webViewSingleton.instance.loadUrl("file:///android_asset/app_config/configure.html?rand=" + Math.random() * 500);
                }
            });
        }

    }

    public static void appMessage(GBDeviceEventAppMessage message) {

        final String jsEvent;
        if (webViewSingleton.instance == null) {
            LOG.warn("WEBVIEW is not initialized, cannot send appMessages to it");
            return;
        }

        if (!message.appUUID.equals(currentRunningUUID)) {
            LOG.info("WEBVIEW ignoring message for app that is not currently running: " + message.appUUID + " message: " + message.message + " type: " + message.type);
            return;
        }

        // TODO: handle ACK and NACK types with ids
        if (message.type != GBDeviceEventAppMessage.TYPE_APPMESSAGE) {
            jsEvent = (GBDeviceEventAppMessage.TYPE_NACK == GBDeviceEventAppMessage.TYPE_APPMESSAGE) ? "NACK" + message.id : "ACK" + message.id;
            LOG.debug("WEBVIEW received ACK/NACK:" + message.message + " for uuid: " + message.appUUID + " ID: " + message.id);
        } else {
            jsEvent = "appmessage";
        }

        final String appMessage = parseIncomingAppMessage(message.message, message.appUUID);
        LOG.debug("to WEBVIEW: event: " + jsEvent + " message: " + appMessage);
        new Handler(webViewSingleton.mainLooper).post(new Runnable() {
            @Override
            public void run() {
                webViewSingleton.instance.evaluateJavascript("Pebble.evaluate('" + jsEvent + "',[" + appMessage + "]);", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        //TODO: the message should be acked here instead of in PebbleIoThread
                        LOG.debug("Callback from appmessage: " + s);
                    }
                });
            }
        });
    }

    public static void disposeWebView() {
        if (internetHelperBound) {
            LOG.debug("WEBVIEW: will unbind the internet helper");
            webViewSingleton.contextWrapper.getApplicationContext().unbindService(internetHelperConnection);
        }
        new Handler(webViewSingleton.mainLooper).post(new Runnable() {
            @Override
            public void run() {
                if (webViewSingleton.instance != null) {
                    webViewSingleton.instance.setWebChromeClient(null);
                    webViewSingleton.instance.setWebViewClient(null);
                    webViewSingleton.instance.clearHistory();
                    webViewSingleton.instance.clearCache(true);
                    webViewSingleton.instance.loadUrl("about:blank");
//                    webViewSingleton.instance.freeMemory();
                    webViewSingleton.instance.pauseTimers();
//                    instance.destroy();
//                    instance = null;
//                    contextWrapper = null;
//                    jsInterface = null;
                }
            }
        });
    }

    private static class CurrentPosition {
        long timestamp;
        double altitude;
        float latitude, longitude, accuracy, speed;

        private CurrentPosition() {
            Prefs prefs = GBApplication.getPrefs();
            this.latitude = prefs.getFloat("location_latitude", 0);
            this.longitude = prefs.getFloat("location_longitude", 0);
            LOG.info("got longitude/latitude from preferences: " + latitude + "/" + longitude);

            this.timestamp = System.currentTimeMillis() - 86400000; //let accessor know this value is really old

            if (ActivityCompat.checkSelfPermission(GBApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    prefs.getBoolean("use_updated_location_if_available", false)) {
                LocationManager locationManager = (LocationManager) GBApplication.getContext().getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria, false);
                if (provider != null) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                    if (lastKnownLocation != null) {
                        this.timestamp = lastKnownLocation.getTime();
                        this.timestamp = System.currentTimeMillis() - 1000; //TODO: request updating the location and don't fake its age

                        this.latitude = (float) lastKnownLocation.getLatitude();
                        this.longitude = (float) lastKnownLocation.getLongitude();
                        this.accuracy = lastKnownLocation.getAccuracy();
                        this.altitude = (float) lastKnownLocation.getAltitude();
                        this.speed = lastKnownLocation.getSpeed();
                    }
                }
            }
        }
    }

    private static WebResourceResponse mimicKiezelPayResponse() {
        return null;
    }

    private static JSONObject coordObject(CurrentPosition currentPosition) throws JSONException {
        JSONObject coord = new JSONObject();
        coord.put("lat", currentPosition.latitude);
        coord.put("lon", currentPosition.longitude);
        return coord;
    }

    private static JSONObject sysObject(CurrentPosition currentPosition) throws JSONException {
        GregorianCalendar[] sunrise = SPA.calculateSunriseTransitSet(new GregorianCalendar(), currentPosition.latitude, currentPosition.longitude, DeltaT.estimate(new GregorianCalendar()));

        JSONObject sys = new JSONObject();
        sys.put("country", "World");
        sys.put("sunrise", (sunrise[0].getTimeInMillis() / 1000));
        sys.put("sunset", (sunrise[2].getTimeInMillis() / 1000));

        return sys;
    }

    private static void convertTemps(JSONObject main, String units) throws JSONException {
        if ("metric".equals(units)) {
            main.put("temp", (int) main.get("temp") - 273);
            main.put("temp_min", (int) main.get("temp_min") - 273);
            main.put("temp_max", (int) main.get("temp_max") - 273);
        } else if ("imperial".equals(units)) { //it's 2017... this is so sad
            main.put("temp", ((int) (main.get("temp")) - 273.15f) * 1.8f + 32);
            main.put("temp_min", ((int) (main.get("temp_min")) - 273.15f) * 1.8f + 32);
            main.put("temp_max", ((int) (main.get("temp_max")) - 273.15f) * 1.8f + 32);
        }
    }

    private static WebResourceResponse mimicOpenWeatherMapResponse(String type, String units) {

        if (Weather.getInstance() == null || Weather.getInstance().getWeather2() == null) {
            LOG.warn("WEBVIEW - Weather instance is null, cannot update weather");
            return null;
        }

        CurrentPosition currentPosition = new CurrentPosition();

        try {
            JSONObject resp;

            if ("/data/2.5/weather".equals(type) && Weather.getInstance().getWeather2().reconstructedWeather != null) {
                resp = new JSONObject(Weather.getInstance().getWeather2().reconstructedWeather.toString());

                JSONObject main = resp.getJSONObject("main");

                convertTemps(main, units); //caller might want different units

                resp.put("cod", 200);
                resp.put("coord", coordObject(currentPosition));
                resp.put("sys", sysObject(currentPosition));
//            } else if ("/data/2.5/forecast".equals(type) && Weather.getInstance().getWeather2().reconstructedForecast != null) { //this is wrong, as we only have daily data. Unfortunately it looks like daily forecasts cannot be reconstructed
//                resp = new JSONObject(Weather.getInstance().getWeather2().reconstructedForecast.toString());
//
//                JSONObject city = resp.getJSONObject("city");
//                city.put("coord", coordObject(currentPosition));
//
//                JSONArray list = resp.getJSONArray("list");
//                for (int i = 0, size = list.length(); i < size; i++) {
//                    JSONObject item = list.getJSONObject(i);
//                    JSONObject main = item.getJSONObject("main");
//                    convertTemps(main, units); //caller might want different units
//                }
//
//                resp.put("cod", 200);
            } else {
                LOG.warn("WEBVIEW - cannot mimick request of type " + type + " (unsupported or lack of data)");
                return null;
            }

            LOG.info("WEBVIEW - mimic openweather response" + resp.toString());
            Map<String, String> headers = new HashMap<>();
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
            LOG.warn(e.getMessage());
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
            Uri parsedUri = request.getUrl();
            LOG.debug("WEBVIEW shouldInterceptRequest URL: " + parsedUri.toString());
            WebResourceResponse mimickedReply = mimicReply(parsedUri);
            if (mimickedReply != null)
                return mimickedReply;
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            LOG.debug("WEBVIEW shouldInterceptRequest URL (legacy): " + url);
            Uri parsedUri = Uri.parse(url);
            WebResourceResponse mimickedReply = mimicReply(parsedUri);
            if (mimickedReply != null)
                return mimickedReply;
            return super.shouldInterceptRequest(view, url);
        }

        private WebResourceResponse mimicReply(Uri requestedUri) {
            if (requestedUri.getHost() != null && (requestedUri.getHost().contains("openweathermap.org") || requestedUri.getHost().contains("tagesschau.de") )) {
                if (internetHelperBound) {
                    LOG.debug("WEBVIEW forwarding request to the internet helper");
                    Bundle bundle = new Bundle();
                    bundle.putString("URL", requestedUri.toString());
                    Message webRequest = Message.obtain();
                    webRequest.replyTo = internetHelperListener;
                    webRequest.setData(bundle);
                    try {
                        latch = new CountDownLatch(1); //the messenger should run on a single thread, hence we don't need to be worried about concurrency. This approach however is certainly not ideal.
                        internetHelper.send(webRequest);
                        latch.await();
                        return internetResponse;

                    } catch (RemoteException | InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    LOG.debug("WEBVIEW request to openweathermap.org detected of type: " + requestedUri.getPath() + " params: " + requestedUri.getQuery());
                    return mimicOpenWeatherMapResponse(requestedUri.getPath(), requestedUri.getQueryParameter("units"));
                }
            } else {
                LOG.debug("WEBVIEW request:" + requestedUri.toString() + " not intercepted");
            }
            return null;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri parsedUri = Uri.parse(url);

            if (parsedUri.getScheme().startsWith("http")) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                GBApplication.getContext().startActivity(i);
            } else if (parsedUri.getScheme().startsWith("pebblejs")) {
                url = url.replaceFirst("^pebblejs://close#", "file:///android_asset/app_config/configure.html?config=true&json=");
                view.loadUrl(url);
            } else if (parsedUri.getScheme().equals("data")) { //clay
                view.loadUrl(url);
            } else {
                    LOG.debug("WEBVIEW Ignoring unhandled scheme: " + parsedUri.getScheme());
            }

            return true;
        }
    }

    private static JSONObject getAppConfigurationKeys(UUID uuid) {
        try {
            File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
            File configurationFile = new File(destDir, uuid.toString() + ".json");
            if (configurationFile.exists()) {
                String jsonString = FileUtils.getStringFromFile(configurationFile);
                JSONObject json = new JSONObject(jsonString);
                return json.getJSONObject("appKeys");
            }
        } catch (IOException | JSONException e) {
            LOG.warn(e.getMessage());
        }
        return null;
    }

    private static String parseIncomingAppMessage(String msg, UUID uuid) {
        JSONObject jsAppMessage = new JSONObject();

        JSONObject knownKeys = getAppConfigurationKeys(uuid);
        SparseArray<String> appKeysMap = new SparseArray<>();

        if (knownKeys == null) {
            return "{}";
        }

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

        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        return jsAppMessage.toString();
    }

    private static class JSInterface {

        UUID mUuid;
        GBDevice device;
        Integer lastTransaction;

        private JSInterface(GBDevice device, UUID mUuid) {
            LOG.debug("Creating JS interface for UUID: " + mUuid.toString());
            this.device = device;
            this.mUuid = mUuid;
            this.lastTransaction = 0;
        }


        private boolean isLocationEnabledForWatchApp() {
            return true; //as long as we don't give watchapp internet access it's not a problem
        }

        @JavascriptInterface
        public void gbLog(String msg) {
            LOG.debug("WEBVIEW webpage log: " + msg);
        }

        @JavascriptInterface
        public String sendAppMessage(String msg, String needsTransactionMsg) {
            boolean needsTransaction = "true".equals(needsTransactionMsg);
            LOG.debug("from WEBVIEW: " + msg + " needs a transaction: " + needsTransaction);
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
                        out.put(outKey, obj);
                    } else {
                        GB.toast("Discarded key " + inKey + ", not found in the local configuration and is not an integer key.", Toast.LENGTH_SHORT, GB.WARN);
                    }

                }
                LOG.info("WEBVIEW message to pebble: " + out.toString());
                if (needsTransaction) {
                    this.lastTransaction++;
                    GBApplication.deviceService().onAppConfiguration(this.mUuid, out.toString(), this.lastTransaction);
                    return this.lastTransaction.toString();
                } else {
                    GBApplication.deviceService().onAppConfiguration(this.mUuid, out.toString(), null);
                }

            } catch (JSONException e) {
                LOG.warn(e.getMessage());
            }
            return null;
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
                LOG.warn(e.getMessage());
            }
            //Json not supported apparently, we need to cast back and forth
            return wi.toString();
        }

        @JavascriptInterface
        public String getAppConfigurationFile() {
            LOG.debug("WEBVIEW loading config file of " + this.mUuid.toString());
            try {
                File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
                File configurationFile = new File(destDir, this.mUuid.toString() + "_config.js");
                if (configurationFile.exists()) {
                    return "file:///" + configurationFile.getAbsolutePath();
                }
            } catch (IOException e) {
                LOG.warn(e.getMessage());
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
                LOG.warn(e.getMessage());
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
                LOG.warn(e.getMessage());
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
                for (byte aByte : bytes) {
                    sb.append(String.format("%02X", aByte));
                }
                return sb.toString().toLowerCase();
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                LOG.warn(e.getMessage());
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
                LOG.warn(e.getMessage());
            }
            LOG.info("WEBVIEW - geo position" + geoPosition.toString());
            return geoPosition.toString();
        }

        @JavascriptInterface
        public void eventFinished(String event) {
            LOG.debug("WEBVIEW event finished: " + event);
        }
    }

}

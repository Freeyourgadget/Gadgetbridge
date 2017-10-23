package nodomain.freeyourgadget.gadgetbridge.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppMessage;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.GBChromeClient;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.GBWebClient;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.JSInterface;

public class WebViewSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(WebViewSingleton.class);

    private WebView instance = null;
    private MutableContextWrapper contextWrapper;
    private Looper mainLooper;
    private static WebViewSingleton webViewSingleton = new WebViewSingleton();
    private static UUID currentRunningUUID;
    public static Messenger internetHelper = null;
    public static boolean internetHelperBound;
    public static CountDownLatch latch;
    public static WebResourceResponse internetResponse;
    public final static Messenger internetHelperListener = new Messenger(new IncomingHandler());

    private WebViewSingleton() {
    }

    public static synchronized void ensureCreated(Activity context) {
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
        }
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
    private static class IncomingHandler extends Handler {

        private String getCharsetFromHeaders(String contentType) {
            if (contentType != null && contentType.toLowerCase().trim().contains("charset=")) {
                String[] parts = contentType.toLowerCase().trim().split("=");
                if (parts.length > 0)
                    return parts[1];
            }
            return null;
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            LOG.debug("WEBVIEW: internet helper returned: " + data.getString("response"));
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                internetResponse = new WebResourceResponse(data.getString("content-type"), data.getString("content-encoding"), 200, "OK",
                        headers,
                        new ByteArrayInputStream(data.getString("response").getBytes(Charset.forName(getCharsetFromHeaders(data.getString("content-type")))))
                );
            } else {
                internetResponse = new WebResourceResponse(data.getString("content-type"), data.getString("content-encoding"), new ByteArrayInputStream(data.getString("response").getBytes(Charset.forName(getCharsetFromHeaders(data.getString("content-type"))))));
            }

            latch.countDown();
        }
    }

    @NonNull
    public static WebView getWebView(Context context) {
        webViewSingleton.contextWrapper.setBaseContext(context);
        return webViewSingleton.instance;
    }

    public static void runJavascriptInterface(GBDevice device, UUID uuid) {
        if (uuid == null && device == null) {
            throw new RuntimeException("Javascript interface started without device and uuid");
        }
        if (uuid.equals(currentRunningUUID)) {
            LOG.debug("WEBVIEW uuid not changed keeping the old context");
        } else {
            final JSInterface jsInterface = new JSInterface(device, uuid);
            LOG.debug("WEBVIEW uuid changed, restarting");
            currentRunningUUID = uuid;
            new Handler(webViewSingleton.mainLooper).post(new Runnable() {
                @Override
                public void run() {
                    webViewSingleton.instance.onResume();
                    webViewSingleton.instance.removeJavascriptInterface("GBjs");
                    webViewSingleton.instance.addJavascriptInterface(jsInterface, "GBjs");
                    webViewSingleton.instance.loadUrl("file:///android_asset/app_config/configure.html?rand=" + Math.random() * 500);
                }
            });
            if (!internetHelperBound) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("nodomain.freeyourgadget.internethelper", "nodomain.freeyourgadget.internethelper.MyService"));
                webViewSingleton.contextWrapper.getApplicationContext().bindService(intent, internetHelperConnection, Context.BIND_AUTO_CREATE);
            }
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

    public static void stopJavascriptInterface() {
        new Handler(webViewSingleton.mainLooper).post(new Runnable() {
            @Override
            public void run() {
                if (webViewSingleton.instance != null) {
                    webViewSingleton.instance.removeJavascriptInterface("GBjs");
                    webViewSingleton.instance.loadUrl("about:blank");
                }
            }
        });
    }

    public static void disposeWebView() {
        if (internetHelperBound) {
            LOG.debug("WEBVIEW: will unbind the internet helper");
            webViewSingleton.contextWrapper.getApplicationContext().unbindService(internetHelperConnection);
            internetHelperBound = false;
        }
        currentRunningUUID = null;
        new Handler(webViewSingleton.mainLooper).post(new Runnable() {
            @Override
            public void run() {
                if (webViewSingleton.instance != null) {
                    webViewSingleton.instance.removeJavascriptInterface("GBjs");
//                    webViewSingleton.instance.setWebChromeClient(null);
//                    webViewSingleton.instance.setWebViewClient(null);
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

    public static JSONObject getAppConfigurationKeys(UUID uuid) {
        try {
            File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
            File configurationFile = new File(destDir, uuid.toString() + ".json");
            if (configurationFile.exists()) {
                String jsonString = FileUtils.getStringFromFile(configurationFile);
                JSONObject json = new JSONObject(jsonString);
                return json.getJSONObject("appKeys");
            }
        } catch (IOException | JSONException e) {
            LOG.warn("Unable to parse configuration JSON file", e);
        }
        return null;
    }

    private static String parseIncomingAppMessage(String msg, UUID uuid) {
        JSONObject jsAppMessage = new JSONObject();

        JSONObject knownKeys = getAppConfigurationKeys(uuid);
        SparseArray<String> appKeysMap = new SparseArray<>();

        if (knownKeys == null || msg == null) {
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
            LOG.warn("Unable to parse incoming app message", e);
        }
        return jsAppMessage.toString();
    }

}

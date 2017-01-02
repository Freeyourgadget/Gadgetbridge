package nodomain.freeyourgadget.gadgetbridge.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class WebViewSingleton extends Activity {

    private static final Logger LOG = LoggerFactory.getLogger(WebViewSingleton.class);

    private static WebView instance = null;
    private static JSInterface jsInterface;
    private static MutableContextWrapper contextWrapper = null;

    private WebViewSingleton() {
    }

    public static WebView getorInitWebView(Context context, GBDevice device, UUID uuid) {
        if (context instanceof Activity) {
            if (contextWrapper != null) {
                contextWrapper.setBaseContext(context);
            } else {
                contextWrapper = new MutableContextWrapper(context);
            }

            if (instance == null) {
                instance = new WebView(contextWrapper);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
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
                });
            }

        } else {
            LOG.debug("WEBV: not using the passed context, as it is not an activity");
        }

        if (jsInterface == null || (jsInterface != null && (!device.equals(jsInterface.device) || !uuid.equals(jsInterface.mUuid)))) {
            jsInterface = new JSInterface(device, uuid);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (instance != null) {
                        instance.removeJavascriptInterface("GBjs");
                        instance.addJavascriptInterface(jsInterface, "GBjs");
                        instance.loadUrl("file:///android_asset/app_config/configure.html");
                    }
                }
            });
        } else {
            LOG.debug("Not reloading the webview " + jsInterface.mUuid.toString());
        }

        return instance;
    }

    public static void appMessage(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    instance.evaluateJavascript("Pebble.evaluate('appmessage',[{'payload':" + message + "}]);", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            LOG.debug("Callback from showConfiguration", s);
                        }
                    });
                } else {
                    instance.loadUrl("javascript:Pebble.evaluate('appmessage',[{'payload':" + message + "}]);");
                }
            }
        });
    }

    public static void disposeWebView() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (instance != null) {
                    instance.destroy();
                    instance = null;
                    contextWrapper = null;
                    jsInterface = null;
                }
            }
        });
    }

    private static class GBChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (ConsoleMessage.MessageLevel.ERROR.equals(consoleMessage.messageLevel())) {
                GB.toast(consoleMessage.message(), Toast.LENGTH_LONG, GB.ERROR);
                //TODO: show error page
            }
            return super.onConsoleMessage(consoleMessage);
        }

    }

    private static class GBWebClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LOG.debug("WEBVIEW shouldInterceptRequest URL" + request.getUrl());
            }
            LOG.debug("WEBVIEW request:" + request.toString());
            return super.shouldInterceptRequest(view, request);
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
            LOG.debug("Creating JS interface");
            this.device = device;
            this.mUuid = mUuid;
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

    }

}

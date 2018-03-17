/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Lem Dulfo, Uwe Hermann

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

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
    public static Messenger internetHelperListener;
    private static boolean internetHelperInstalled;

    private WebViewSingleton() {
    }

    public static synchronized void ensureCreated(Activity context) {
        if (webViewSingleton.instance == null) {
            webViewSingleton.contextWrapper = new MutableContextWrapper(context);
            webViewSingleton.mainLooper = webViewSingleton.contextWrapper.getMainLooper();
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

    /**
     * Checks that the webview is up and the given app is running.
     * @param uuid the uuid of the application expected to be running
     * @throws IllegalStateException when the webview is not active or the app is not running
     */
    public static void checkAppRunning(@NonNull UUID uuid) {
        if (webViewSingleton.instance == null) {
            throw new IllegalStateException("webViewSingleton.instance is null!");
        }
        if (!uuid.equals(currentRunningUUID)) {
            throw new IllegalStateException("Expected app " + uuid + " is not running, but " + currentRunningUUID + " is.");
        }
    }

    public static void runJavascriptInterface(@NonNull Activity context, @NonNull GBDevice device, @NonNull UUID uuid) {
        if (webViewSingleton.instance == null || webViewSingleton.contextWrapper == null) {
            ensureCreated(context);
        }
        runJavascriptInterface(device, uuid);
    }

    public static void runJavascriptInterface(@NonNull GBDevice device, @NonNull UUID uuid) {
        if (uuid.equals(currentRunningUUID)) {
            LOG.debug("WEBVIEW uuid not changed keeping the old context");
        } else {
            final JSInterface jsInterface = new JSInterface(device, uuid);
            LOG.debug("WEBVIEW uuid changed, restarting");
            currentRunningUUID = uuid;
            invokeWebview(new WebViewRunnable() {
                @Override
                public void invoke(WebView webView) {
                    webView.onResume();
                    webView.removeJavascriptInterface("GBjs");
                    webView.addJavascriptInterface(jsInterface, "GBjs");
                    webView.loadUrl("file:///android_asset/app_config/configure.html?rand=" + Math.random() * 500);
                }
            });
            if (!internetHelperBound && !internetHelperInstalled) {
                String internetHelperPkg = "nodomain.freeyourgadget.internethelper";
                String internetHelperCls = internetHelperPkg + ".MyService";
                try {
                    webViewSingleton.contextWrapper.getPackageManager().getApplicationInfo(internetHelperPkg, 0);
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(internetHelperPkg, internetHelperCls));
                    webViewSingleton.contextWrapper.getApplicationContext().bindService(intent, internetHelperConnection, Context.BIND_AUTO_CREATE);
                    internetHelperListener = new Messenger(new IncomingHandler());
                    internetHelperInstalled = true;
                }
                catch (PackageManager.NameNotFoundException e) {
                    internetHelperInstalled = false;
                    LOG.info("WEBVIEW: Internet helper not installed, only mimicked HTTP requests will work.");
                }
            }
        }

    }

    public static void stopJavascriptInterface() {
        invokeWebview(new WebViewRunnable() {
            @Override
            public void invoke(WebView webView) {
                webView.removeJavascriptInterface("GBjs");
                webView.loadUrl("about:blank");
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
        invokeWebview(new WebViewRunnable() {
            @Override
            public void invoke(WebView webView) {
                webView.removeJavascriptInterface("GBjs");
//                    webView.setWebChromeClient(null);
//                    webView.setWebViewClient(null);
                webView.clearHistory();
                webView.clearCache(true);
                webView.loadUrl("about:blank");
//                    webView.freeMemory();
                webView.pauseTimers();
//                    instance.destroy();
//                    instance = null;
//                    contextWrapper = null;
//                    jsInterface = null;
            }
        });
    }

    public static void invokeWebview(final WebViewRunnable runnable) {
        if (webViewSingleton.instance == null || webViewSingleton.mainLooper == null) {
            LOG.warn("Webview already disposed, ignoring runnable");
            return;
        }
        new Handler(webViewSingleton.mainLooper).post(new Runnable() {
            @Override
            public void run() {
                if (webViewSingleton.instance == null) {
                    LOG.warn("Webview already disposed, cannot invoke runnable");
                    return;
                }
                runnable.invoke(webViewSingleton.instance);
            }
        });
    }

    public interface WebViewRunnable {
        /**
         * Called in the main thread with a non-null webView instance
         * @param webView the webview, never null
         */
        void invoke(WebView webView);
    }
}

/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.app.NavUtils;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.GBChromeClient;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.GBWebClient;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.JSInterface;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.WebViewSingleton;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;

public class ExternalPebbleJSActivity extends AbstractGBActivity {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalPebbleJSActivity.class);

    private Uri confUri;
    /**
     * When bgjs is enabled, this field refers to the WebViewSingleton,
     * otherwise it refers to the legacy webview from the activity_legacy_external_pebble_js layout
     */
    private WebView myWebView;
    public static final String START_BG_WEBVIEW = "start_webview";
    public static final String SHOW_CONFIG = "configure";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        boolean showConfig = false;

        UUID currentUUID = null;
        GBDevice currentDevice = null;

        if (extras == null) {
            confUri = getIntent().getData();
            if(confUri.getScheme().equals("gadgetbridge")) {
                try {
                    currentUUID = UUID.fromString(confUri.getHost());
                } catch (IllegalArgumentException e) {
                    LOG.error("UUID in incoming configuration is not a valid UUID: " +confUri.toString());
                }

                //first check if we are still connected to a pebble
                DeviceManager deviceManager = ((GBApplication) getApplication()).getDeviceManager();
                List<GBDevice> deviceList = deviceManager.getDevices();
                for (GBDevice device : deviceList) {
                    if (device.getState() == GBDevice.State.INITIALIZED) {
                        if (device.getType().equals(DeviceType.PEBBLE)) {
                            currentDevice = device;
                            break;
                        } else {
                            LOG.error("attempting to load pebble configuration but a different device type is connected!!!");
                            finish();
                            return;
                        }
                    }
                }
                if (currentDevice == null) {
                    //then try to reconnect to last connected device
                    String btDeviceAddress = GBApplication.getPrefs().getPreferences().getString("last_device_address", null);
                    if (btDeviceAddress != null) {
                        GBDevice candidate = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress, this);
                        if(!candidate.isConnected() && candidate.getType() == DeviceType.PEBBLE){
                            Intent intent = new Intent(this, DeviceCommunicationService.class)
                                    .setAction(ACTION_CONNECT)
                                    .putExtra(GBDevice.EXTRA_DEVICE, currentDevice);
                            this.startService(intent);
                            currentDevice = candidate;
                        }
                    }
                }

                showConfig = true; //we are getting incoming configuration data
            }
        } else {
            currentDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
            currentUUID = (UUID) extras.getSerializable(DeviceService.EXTRA_APP_UUID);

            if (extras.getBoolean(START_BG_WEBVIEW, false)) {
                startBackgroundWebViewAndFinish();
                return;
            }
            showConfig = extras.getBoolean(SHOW_CONFIG, false);
        }

        if (GBApplication.getGBPrefs().isBackgroundJsEnabled()) {
            if (showConfig) {
                Objects.requireNonNull(currentDevice, "Must provide a device when invoking this activity");
                Objects.requireNonNull(currentUUID, "Must provide a uuid when invoking this activity");
                WebViewSingleton.getInstance().runJavascriptInterface(this, currentDevice, currentUUID);
            }

            // FIXME: is this really supposed to be outside the check for SHOW_CONFIG?
            setupBGWebView();
        } else {
            Objects.requireNonNull(currentDevice, "Must provide a device when invoking this activity without bgjs");
            Objects.requireNonNull(currentUUID, "Must provide a uuid when invoking this activity without bgjs");
            setupLegacyWebView(currentDevice, currentUUID);
        }
    }

    private void startBackgroundWebViewAndFinish() {
        if (GBApplication.getGBPrefs().isBackgroundJsEnabled()) {
            WebViewSingleton.ensureCreated(this);
        } else {
            LOG.warn("BGJs disabled, not starting webview");
        }
        finish();
    }

    private void setupBGWebView() {
        setContentView(R.layout.activity_external_pebble_js);
        myWebView = WebViewSingleton.getInstance().getWebView(this);
        if (myWebView.getParent() != null) {
            ((ViewGroup) myWebView.getParent()).removeView(myWebView);
        }
        myWebView.setWillNotDraw(false);
        myWebView.removeJavascriptInterface("GBActivity");
        myWebView.addJavascriptInterface(new ActivityJSInterface(), "GBActivity");
        FrameLayout fl = (FrameLayout) findViewById(R.id.webview_placeholder);
        fl.addView(myWebView);

        myWebView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                v.removeOnAttachStateChangeListener(this);
                FrameLayout fl = (FrameLayout) findViewById(R.id.webview_placeholder);
                fl.removeAllViews();
            }
        });
    }

    private void setupLegacyWebView(@NonNull GBDevice device, @NonNull UUID uuid) {
        setContentView(R.layout.activity_legacy_external_pebble_js);
        myWebView = (WebView) findViewById(R.id.configureWebview);
        myWebView.clearCache(true);
        myWebView.setWebViewClient(new GBWebClient());
        myWebView.setWebChromeClient(new GBChromeClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //needed to access the DOM
        webSettings.setDomStorageEnabled(true);
        //needed for localstorage
        webSettings.setDatabaseEnabled(true);

        JSInterface gbJSInterface = new JSInterface(device, uuid);
        myWebView.addJavascriptInterface(gbJSInterface, "GBjs");
        myWebView.addJavascriptInterface(new ActivityJSInterface(), "GBActivity");

        myWebView.loadUrl("file:///android_asset/app_config/configure.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        String queryString = "";
        if (confUri != null) {
            //getting back with configuration data
            LOG.debug("WEBVIEW returned config: " + confUri.toString());
            try {
                queryString = confUri.getEncodedQuery();
            } catch (IllegalArgumentException e) {
                GB.toast("returned uri: " + confUri.toString(), Toast.LENGTH_LONG, GB.ERROR);
            }
            myWebView.stopLoading();
            myWebView.loadUrl("file:///android_asset/app_config/configure.html?" + queryString);
        }
    }
    @Override
    protected void onNewIntent(Intent incoming) {
        incoming.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        super.onNewIntent(incoming);
        confUri = incoming.getData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ActivityJSInterface {

        @JavascriptInterface
        public void closeActivity() {
            NavUtils.navigateUpFromSameTask(ExternalPebbleJSActivity.this);
        }
    }
}

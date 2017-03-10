/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
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
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ExternalPebbleJSActivity extends GBActivity {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalPebbleJSActivity.class);

    private UUID appUuid;
    private Uri confUri;
    private GBDevice mGBDevice = null;
    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
            appUuid = (UUID) extras.getSerializable(DeviceService.EXTRA_APP_UUID);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }


        setContentView(R.layout.activity_external_pebble_js);

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

        JSInterface gbJSInterface = new JSInterface(this);
        myWebView.addJavascriptInterface(gbJSInterface, "GBjs");

        myWebView.loadUrl("file:///android_asset/app_config/configure.html");

    }

    @Override
    protected void onNewIntent(Intent incoming) {
        incoming.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        super.onNewIntent(incoming);
        confUri = incoming.getData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String queryString = "";

        if (confUri != null) {
            //getting back with configuration data
            try {
                appUuid = UUID.fromString(confUri.getHost());
                queryString = confUri.getEncodedQuery();
            } catch (IllegalArgumentException e) {
                GB.toast("returned uri: " + confUri.toString(), Toast.LENGTH_LONG, GB.ERROR);
            }
            myWebView.loadUrl("file:///android_asset/app_config/configure.html?" + queryString);
        }

    }

    private JSONObject getAppConfigurationKeys() {
        try {
            File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
            File configurationFile = new File(destDir, appUuid.toString() + ".json");
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

    private boolean isLocationEnabledForWatchApp() {
        return true; //as long as we don't give watchapp internet access it's not a problem
    }

    private class GBChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (ConsoleMessage.MessageLevel.ERROR.equals(consoleMessage.messageLevel())) {
                GB.toast(consoleMessage.message(), Toast.LENGTH_LONG, GB.ERROR);
                //TODO: show error page
            }
            return super.onConsoleMessage(consoleMessage);
        }

    }

    private class GBWebClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } else {
                url = url.replaceFirst("^pebblejs://close#", "file:///android_asset/app_config/configure.html?config=true&json=");
                view.loadUrl(url);
            }

            return true;

        }
    }

    private class JSInterface {

        Context mContext;

        public JSInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void gbLog(String msg) {
            Log.d("WEBVIEW", msg);
        }

        @JavascriptInterface
        public void sendAppMessage(String msg) {
            LOG.debug("from WEBVIEW: " + msg);
            JSONObject knownKeys = getAppConfigurationKeys();

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
                LOG.info(out.toString());
                GBApplication.deviceService().onAppConfiguration(appUuid, out.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String getActiveWatchInfo() {
            JSONObject wi = new JSONObject();
            try {
                wi.put("firmware", mGBDevice.getFirmwareVersion());
                wi.put("platform", PebbleUtils.getPlatformName(mGBDevice.getModel()));
                wi.put("model", PebbleUtils.getModel(mGBDevice.getModel()));
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
                File configurationFile = new File(destDir, appUuid.toString() + "_config.js");
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
                File configurationFile = new File(destDir, appUuid.toString() + "_preset.json");
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
                File presetsFile = new File(destDir, appUuid.toString() + "_preset.json");
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
            return appUuid.toString();
        }

        @JavascriptInterface
        public String getAppLocalstoragePrefix() {
            String prefix = mGBDevice.getAddress() + appUuid.toString();
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
            return "gb" + appUuid.toString();
        }

        @JavascriptInterface
        public void closeActivity() {
            NavUtils.navigateUpFromSameTask((ExternalPebbleJSActivity) mContext);
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

                Prefs prefs = GBApplication.getPrefs();

                geoPosition.put("timestamp", (System.currentTimeMillis() / 1000) - 86400); //let JS know this value is really old

                coords.put("latitude", prefs.getFloat("location_latitude", 0));
                coords.put("longitude", prefs.getFloat("location_longitude", 0));

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        prefs.getBoolean("use_updated_location_if_available", false)) {
                    LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    String provider = locationManager.getBestProvider(criteria, false);
                    if (provider != null) {
                        Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                        if (lastKnownLocation != null) {
                            geoPosition.put("timestamp", lastKnownLocation.getTime());

                            coords.put("latitude", (float) lastKnownLocation.getLatitude());
                            coords.put("longitude", (float) lastKnownLocation.getLongitude());
                            coords.put("accuracy", lastKnownLocation.getAccuracy());
                            coords.put("altitude", lastKnownLocation.getAltitude());
                            coords.put("speed", lastKnownLocation.getSpeed());
                        }
                    }
                }

                geoPosition.put("coords", coords);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return geoPosition.toString();
        }
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


}

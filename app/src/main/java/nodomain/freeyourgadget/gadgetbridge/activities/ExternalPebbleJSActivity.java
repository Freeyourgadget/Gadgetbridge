package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;

public class ExternalPebbleJSActivity extends Activity {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalPebbleJSActivity.class);

    private UUID appUuid;
    private GBDevice mGBDevice = null;

    private void handleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }

        String queryString = "";
        Uri uri = intent.getData();

        if (uri != null) {
            //getting back with configuration data
            if (uri.getScheme().equals("gadgetbridge")) {
                appUuid = UUID.fromString(uri.getHost());
            }
            if (uri.getScheme().equals("pebblejs")) {
                queryString = uri.toString().replace("pebblejs://close#", "config=true&json="); // evil
            } else {
                queryString = uri.getEncodedQuery();
            }
            LOG.info(uri.toString());
        } else {
            appUuid = (UUID) getIntent().getSerializableExtra("app_uuid");
        }

        WebView myWebView = (WebView) findViewById(R.id.configureWebview);
        myWebView.clearCache(true);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //needed to access the DOM
        webSettings.setDomStorageEnabled(true);

        JSInterface gbJSInterface = new JSInterface();
        myWebView.addJavascriptInterface(gbJSInterface, "GBjs");
        LOG.info(queryString);

        myWebView.loadUrl("file:///android_asset/app_config/configure.html?" + queryString);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_external_pebble_js);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        super.onNewIntent(intent);
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

    private class JSInterface {

        public JSInterface() {
        }

        @JavascriptInterface
        public void gbLog(String msg) {
            Log.d("WEBVIEW", msg);
        }

        @JavascriptInterface
        public void sendAppMessage(String msg) {
            LOG.debug("from WEBVIEW: ", msg);
            JSONObject knownKeys = getAppConfigurationKeys();

            try {
                JSONObject in = new JSONObject(msg);
                JSONObject out = new JSONObject();
                String cur_key;
                for (Iterator<String> key = in.keys(); key.hasNext(); ) {
                    cur_key = key.next();
                    int pebbleAppIndex = knownKeys.optInt(cur_key);
                    if (pebbleAppIndex != 0) {
                        Object obj = in.get(cur_key);
                        if (obj instanceof Boolean) {
                            obj = ((Boolean) obj) ? "true" : "false";
                        }
                        out.put(String.valueOf(pebbleAppIndex), obj);
                    } else {
                        GB.toast("Discarded key " + cur_key + ", not found in the local configuration.", Toast.LENGTH_SHORT, GB.WARN);
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
                wi.put("platform", PebbleUtils.getPlatformName(mGBDevice.getHardwareVersion()));
                wi.put("model", PebbleUtils.getModel(mGBDevice.getHardwareVersion()));
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
        public String getAppUUID() {
            return appUuid.toString();
        }

        @JavascriptInterface
        public String getWatchToken() {
            //specification says: A string that is is guaranteed to be identical for each Pebble device for the same app across different mobile devices. The token is unique to your app and cannot be used to track Pebble devices across applications. see https://developer.pebble.com/docs/js/Pebble/
            return "gb" + appUuid.toString();
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

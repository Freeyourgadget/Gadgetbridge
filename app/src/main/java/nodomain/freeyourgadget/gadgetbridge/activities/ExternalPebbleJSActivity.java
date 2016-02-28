package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ExternalPebbleJSActivity extends Activity {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalPebbleJSActivity.class);

    //TODO: get device
    private Uri uri;
    private UUID appUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String queryString = "";
        uri = getIntent().getData();
        if (uri != null) {
            //getting back with configuration data
            appUuid = UUID.fromString(uri.getHost());
            queryString = uri.getEncodedQuery();
        } else {
            appUuid = (UUID) getIntent().getSerializableExtra("app_uuid");
        }

        setContentView(R.layout.activity_external_pebble_js);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        WebView myWebView = (WebView) findViewById(R.id.configureWebview);
        myWebView.clearCache(true);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //needed to access the DOM
        webSettings.setDomStorageEnabled(true);

        JSInterface gbJSInterface = new JSInterface();
        myWebView.addJavascriptInterface(gbJSInterface, "GBjs");

        myWebView.loadUrl("file:///android_asset/app_config/configure.html?"+queryString);
    }

    private JSONObject getAppConfigurationKeys() {
        try {
            File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
            File configurationFile = new File(destDir, appUuid.toString() + ".json");
            if(configurationFile.exists()) {
                String jsonstring = FileUtils.getStringFromFile(configurationFile);
                JSONObject json = new JSONObject(jsonstring);
                return json.getJSONObject("appKeys");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class JSInterface {

        public JSInterface () {
        }

        @JavascriptInterface
        public void gbLog(String msg) {
            Log.d("WEBVIEW", msg);
        }

        @JavascriptInterface
        public void sendAppMessage(String msg) {
            Log.d("from WEBVIEW", msg);
            JSONObject knownKeys = getAppConfigurationKeys();
            ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();

            try{
                JSONObject in = new JSONObject(msg);
                String cur_key;
                for (Iterator<String> key = in.keys(); key.hasNext();) {
                    cur_key = key.next();
                    int pebbleAppIndex = knownKeys.optInt(cur_key);
                    if (pebbleAppIndex != 0) {
                        //TODO: cast to integer (int32) / String? Is it needed?
                        pairs.add(new Pair<>(pebbleAppIndex, (Object) in.get(cur_key)));
                    } else {
                        GB.toast("Discarded key "+cur_key+", not found in the local configuration.", Toast.LENGTH_SHORT, GB.WARN);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //TODO: send pairs to pebble. (encodeApplicationMessagePush(ENDPOINT_APPLICATIONMESSAGE, uuid, pairs);)
        }

        @JavascriptInterface
        public String getActiveWatchInfo() {
            //TODO: interact with GBDevice, see also todo at the beginning
            JSONObject wi = new JSONObject();
            try {
                wi.put("platform", "basalt");
            }catch (JSONException e) {
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
                if(configurationFile.exists()) {
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
    }

}

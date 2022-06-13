package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_INTERNET_ACCESS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs.BangleJSDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BANGLEJS_WEBVIEW_URL;

public class AppsManagementActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(AppsManagementActivity.class);

    private WebView webView;
    private GBDevice mGBDevice;
    private DeviceCoordinator mCoordinator;
    /// It seems we can get duplicate broadcasts sometimes - so this helps to avoid that
    private int deviceRxSeq = -1;

    public AppsManagementActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banglejs_apps_management);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mGBDevice = bundle.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }
        mCoordinator = DeviceHelper.getInstance().getCoordinator(mGBDevice);
    }

    private void toast(String data) {
        GB.toast(data, Toast.LENGTH_LONG, GB.INFO);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.destroy();
        webView = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceUpdateReceiver);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        commandFilter.addAction(BangleJSDeviceSupport.BANGLEJS_COMMAND_RX);
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceUpdateReceiver, commandFilter);
        initViews();
    }

    BroadcastReceiver deviceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BangleJSDeviceSupport.BANGLEJS_COMMAND_RX: {
                    String data = String.valueOf(intent.getExtras().get("DATA"));
                    int seq = intent.getIntExtra("SEQ",0);
                    LOG.info("WebView TX: " + data + "("+seq+")");
                    if (seq==deviceRxSeq) {
                        LOG.info("WebView TX DUPLICATE AND IGNORED");
                    } else {
                        deviceRxSeq = seq;
                        bangleRxData(data);
                    }
                    break;
                }
            }
        }
    };

    public class WebViewInterface {
        Context mContext;

        WebViewInterface(Context c) {
            mContext = c;
        }

        /// Called from the WebView when data needs to be sent to the Bangle
        @JavascriptInterface
        public void bangleTx(String data) {
            LOG.info("WebView RX: " + data);
            bangleTxData(data);
        }

    }

    // Called when data received from Bangle.js - push data to the WebView
    public void bangleRxData(String data) {
        JSONArray s = new JSONArray();
        s.put(data);
        String ss = s.toString();
        final String js = "bangleRx("+ss.substring(1, ss.length()-1)+");";
        LOG.info("WebView TX cmd: " + js);
        if (webView!=null) webView.post(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(js, null);
                } else {
                    webView.loadUrl("javascript: "+js);
                }
            }
        });
    }

    // Called to send data to Bangle.js
    public void bangleTxData(String data) {
        Intent intent = new Intent(BangleJSDeviceSupport.BANGLEJS_COMMAND_TX);
        intent.putExtra("DATA", data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void initViews() {
        //https://stackoverflow.com/questions/4325639/android-calling-javascript-functions-in-webview
        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        String databasePath = this.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        settings.setDatabasePath(databasePath);
        webView.addJavascriptInterface(new WebViewInterface(this), "Android");
        webView.setWebContentsDebuggingEnabled(true); // FIXME

        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(mGBDevice.getAddress()));
        String url = devicePrefs.getString(PREF_BANGLEJS_WEBVIEW_URL, "").trim();
        if (url.isEmpty()) url = "https://banglejs.com/apps/android.html";
        webView.loadUrl(url);

        webView.setWebViewClient(new WebViewClient(){
            public void onPageFinished(WebView view, String weburl){
                //webView.loadUrl("javascript:showToast('WebView in Espruino')");
            }
        });
    }
}

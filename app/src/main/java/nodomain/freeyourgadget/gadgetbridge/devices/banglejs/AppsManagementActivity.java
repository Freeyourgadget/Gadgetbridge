package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs.BangleJSDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class AppsManagementActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(AppsManagementActivity.class);
    private WebView webView;
    private GBDevice mGBDevice;
    private DeviceCoordinator mCoordinator;

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

        initViews();
    }

    private void toast(String data) {
        GB.toast(data, Toast.LENGTH_LONG, GB.INFO);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceUpdateReceiver);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceUpdateReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
    }

    BroadcastReceiver deviceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };


    public class WebViewInterface {
        Context mContext;

        WebViewInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void bangleTx(String data) {
            LOG.info("WebView RX: " + data);
            bangleRx("Hello world");
        }

    }

    // Called when data received from Bangle.js
    public void bangleRx(String data) {
        JSONArray s = new JSONArray();
        s.put(data);
        String ss = s.toString();
        final String js = "bangleRx("+ss.substring(1, ss.length()-1)+");";
        LOG.info("WebView TX cmd: " + js);
        webView.post(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(js, null);
                }
                else {
                    webView.loadUrl("javascript: "+js);
                }
            }
        });
    }

    private void initViews() {
        //https://stackoverflow.com/questions/4325639/android-calling-javascript-functions-in-webview
        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebViewInterface(this), "Android");
        webView.loadUrl("https://www.pur3.co.uk/tmp/android.html");

        webView.setWebViewClient(new WebViewClient(){
            public void onPageFinished(WebView view, String weburl){
                webView.loadUrl("javascript:showToast('WebView in Espruino')");
            }
        });
    }
}

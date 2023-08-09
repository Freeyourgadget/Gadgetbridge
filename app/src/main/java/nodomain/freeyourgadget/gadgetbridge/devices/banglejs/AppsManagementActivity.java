package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_INTERNET_ACCESS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
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
    /// When a file chooser has been opened in the WebView, this is what should get called
    private ValueCallback<Uri[]> fileChooserCallback;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView!=null) return; // already set up
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        commandFilter.addAction(BangleJSDeviceSupport.BANGLEJS_COMMAND_RX);
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceUpdateReceiver, commandFilter);
        initViews();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        webView = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceUpdateReceiver);
        super.onDestroy();
        finish();
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
                if (webView==null) return; // webView may have gone by the time we get called!
                webView.evaluateJavascript(js, null);
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
            @Override
            public boolean shouldOverrideUrlLoading(WebView vw, WebResourceRequest request) {
                Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                vw.getContext().startActivity(intent);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(AppsManagementActivity.this, "Error:" + description, Toast.LENGTH_SHORT).show();
                view.loadUrl("about:blank");
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView vw, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
                fileChooserCallback = filePathCallback;

                Intent selectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                selectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                String[] acceptedTypes = fileChooserParams.getAcceptTypes();
                if ((acceptedTypes!=null) && acceptedTypes.length>0 && acceptedTypes[0].contains("/"))
                    selectionIntent.setType(acceptedTypes[0]);
                else
                    selectionIntent.setType("*/*");

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, selectionIntent);
                startActivityForResult(chooserIntent, 0);

                return true;
            }


        });
        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                if (url == null) return;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url.replaceFirst("^blob:", "")));
                startActivity(i);

                /*DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url)); //  fails with java.lang.IllegalArgumentException: Can not handle uri::
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "download");
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);*/
            }
        });
    }

    @Override // for file chooser results
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (fileChooserCallback==null) return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK)
            results = new Uri[]{Uri.parse(intent.getDataString())};
        fileChooserCallback.onReceiveValue(results);
        fileChooserCallback = null;
    }

}

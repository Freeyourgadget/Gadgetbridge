package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_INTERNET_ACCESS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
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
import android.webkit.WebResourceResponse;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs.BangleJSDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Capsule;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.internethelper.aidl.http.HttpGetRequest;
import nodomain.freeyourgadget.internethelper.aidl.http.HttpResponse;
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpCallback;
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpService;

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

        final Intent intent1 = new Intent("nodomain.freeyourgadget.internethelper.HttpService");
        intent1.setPackage("nodomain.freeyourgadget.internethelper");
        boolean res = getApplicationContext().bindService(intent1, mHttpConnection, Context.BIND_AUTO_CREATE);
        if (res) {
            LOG.info("Bound to HttpService");
        } else {
            LOG.warn("Could not bind to HttpService");
        }

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

    private volatile IHttpService iHttpService;
    private final CountDownLatch latchInit = new CountDownLatch(1);

    private final ServiceConnection mHttpConnection = new ServiceConnection() {
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            LOG.info("onServiceConnected: {}", className);
            iHttpService = IHttpService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(final ComponentName className) {
            LOG.error("Service has unexpectedly disconnected: {}", className);
            iHttpService = null;
        }
    };

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
        final String url = devicePrefs.getString(PREF_BANGLEJS_WEBVIEW_URL, "https://banglejs.com/apps/android.html").trim();

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
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                LOG.info("shouldIntercept {} {}", request.getUrl(), iHttpService != null);
                if (iHttpService == null) {
                    return super.shouldInterceptRequest(view, request);
                }
                HttpGetRequest httpGetRequest = new HttpGetRequest(request.getUrl().toString(), new Bundle());
                CountDownLatch latch = new CountDownLatch(1);
                final Capsule<WebResourceResponse> internetResponseCapsule = new Capsule<>();
                try {
                    iHttpService.get(httpGetRequest, new IHttpCallback.Stub() {
                        @Override
                        public void onResponse(HttpResponse response) throws RemoteException {
                            WebResourceResponse internetResponse = new WebResourceResponse(
                                    response.getHeaders().getString("content-type"),
                                    response.getHeaders().getString("content-encoding"),
                                    response.getStatus(), "OK",
                                    response.getHeadersMap(),
                                    new ByteArrayInputStream(response.getBody())
                            );
                            internetResponseCapsule.set(internetResponse);
                            latch.countDown();
                        }

                        @Override
                        public void onException(String message) throws RemoteException {
                            throw new RuntimeException(message);
                        }
                    });
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                return internetResponseCapsule.get();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(AppsManagementActivity.this, "Error:" + description, Toast.LENGTH_SHORT).show();
                view.loadUrl("about:blank");
            }
        });

        final Looper mainLooper = Looper.getMainLooper();
        new Handler(mainLooper).postDelayed(() -> {
            webView.loadUrl(url);
        }, 1000);


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

package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.WebViewSingleton;

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


        WebViewSingleton.getorInitWebView(this, mGBDevice, appUuid);

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

        myWebView = WebViewSingleton.getorInitWebView(this, mGBDevice, appUuid);
        myWebView.addJavascriptInterface(new ActivityJSInterface(this), "GBActivity");


        FrameLayout fl = (FrameLayout) findViewById(R.id.webview_placeholder);
        if (myWebView != null)
            fl.addView(myWebView);

        //needed to display clay dialogs
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.requestFocus();


        if (confUri != null) {
            //getting back with configuration data
            try {
                appUuid = UUID.fromString(confUri.getHost());
                queryString = confUri.getEncodedQuery();
            } catch (IllegalArgumentException e) {
                GB.toast("returned uri: " + confUri.toString(), Toast.LENGTH_LONG, GB.ERROR);
            }
            myWebView.loadUrl("file:///android_asset/app_config/configure.html?" + queryString);
        } else {
            //show configuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                myWebView.evaluateJavascript("Pebble.evaluate('showConfiguration');", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        LOG.debug("Callback from showConfiguration", s);
                    }
                });
            } else {
                myWebView.loadUrl("javascript:Pebble.evaluate('showConfiguration');");
            }
        }

    }

    @Override
    protected void onPause() {

        FrameLayout fl = (FrameLayout) findViewById(R.id.webview_placeholder);
        fl.removeAllViews();
        super.onPause();
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

        Context mContext;

        public ActivityJSInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void closeActivity() {
            NavUtils.navigateUpFromSameTask((ExternalPebbleJSActivity) mContext);
        }
    }

}

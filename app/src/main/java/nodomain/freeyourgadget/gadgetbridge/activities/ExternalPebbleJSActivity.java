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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
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

    private Uri confUri;
    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            WebViewSingleton.runJavascriptInterface((GBDevice) extras.getParcelable(GBDevice.EXTRA_DEVICE), (UUID) extras.getSerializable(DeviceService.EXTRA_APP_UUID));
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        setContentView(R.layout.activity_external_pebble_js);

        WebViewSingleton.updateActivityContext(this);
        myWebView = WebViewSingleton.getWebView();
        myWebView.setWillNotDraw(false);
        myWebView.removeJavascriptInterface("GBActivity");
        myWebView.addJavascriptInterface(new ActivityJSInterface(ExternalPebbleJSActivity.this), "GBActivity");
        FrameLayout fl = (FrameLayout) findViewById(R.id.webview_placeholder);
        fl.addView(myWebView);


        myWebView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

                v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                //show configuration - moved to JS
//                myWebView.evaluateJavascript("Pebble.evaluate('showConfiguration');", new ValueCallback<String>() {
//                    @Override
//                    public void onReceiveValue(String s) {
//                        LOG.debug("Callback from showConfiguration: " + s);
//                    }
//                });
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                myWebView.removeJavascriptInterface("GBActivity");
                myWebView.setWillNotDraw(true);
                myWebView.evaluateJavascript("showStep('step1')", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        LOG.debug("Callback from window detach: " + s);
                    }
                });
                FrameLayout fl = (FrameLayout) findViewById(R.id.webview_placeholder);
                fl.removeAllViews();
            }
        });
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

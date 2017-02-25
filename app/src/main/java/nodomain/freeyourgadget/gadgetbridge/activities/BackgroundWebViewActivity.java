package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.WebViewSingleton;

public class BackgroundWebViewActivity extends Activity {
    private static Logger LOG = LoggerFactory.getLogger(BackgroundWebViewActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebViewSingleton.createWebView(this);
        finish();
    }
}

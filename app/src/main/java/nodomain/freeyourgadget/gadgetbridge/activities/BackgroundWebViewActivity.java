package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.os.Bundle;

import nodomain.freeyourgadget.gadgetbridge.util.WebViewSingleton;

public class BackgroundWebViewActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebViewSingleton.getInstance(this);
        finish();
    }
}

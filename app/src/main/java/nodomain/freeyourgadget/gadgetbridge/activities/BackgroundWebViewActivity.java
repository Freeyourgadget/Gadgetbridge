package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;

import nodomain.freeyourgadget.gadgetbridge.util.WebViewSingleton;

public class BackgroundWebViewActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        WebViewSingleton.createWebView(this);
        setVisible(false);
    }
}

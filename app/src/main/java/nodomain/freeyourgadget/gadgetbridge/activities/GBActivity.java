package nodomain.freeyourgadget.gadgetbridge.activities;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;


public class GBActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GBApplication.isDarkThemeEnabled()) {
            setTheme(R.style.GadgetbridgeThemeDark);
        } else {
            setTheme(R.style.GadgetbridgeTheme);
        }

        super.onCreate(savedInstanceState);
    }

    protected DaoSession getDAOSession() {
        return GBApplication.getDaoSession();
    }
}

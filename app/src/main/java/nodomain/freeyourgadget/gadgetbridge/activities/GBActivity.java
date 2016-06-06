package nodomain.freeyourgadget.gadgetbridge.activities;


import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class GBActivity extends AppCompatActivity {
    private void setLanguage(String language) {
        Locale locale;
        if (language.equals("default")) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(language);
        }
        Configuration config = new Configuration();
        config.locale = locale;

        // FIXME: I have no idea what I am doing
        getApplicationContext().getResources().updateConfiguration(config, getApplicationContext().getResources().getDisplayMetrics());
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GBApplication.isDarkThemeEnabled()) {
            setTheme(R.style.GadgetbridgeThemeDark);
        } else {
            setTheme(R.style.GadgetbridgeTheme);
        }

        Prefs prefs = GBApplication.getPrefs();
        String language = prefs.getString("language", "default");
        setLanguage(language);
        super.onCreate(savedInstanceState);
    }
}

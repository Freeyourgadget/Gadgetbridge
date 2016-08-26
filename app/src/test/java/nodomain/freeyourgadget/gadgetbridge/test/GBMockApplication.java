package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.test.mock.MockApplication;

import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class GBMockApplication extends MockApplication {
    private static final String PREF_NAME = "testprefs";
    private final PackageManager mPackageManager;
    private Prefs prefs;
    private GBPrefs gbPrefs;

    public GBMockApplication(PackageManager packageManager) {
        GB.environment = GBEnvironment.createDeviceEnvironment().createLocalTestEnvironment();
        mPackageManager = packageManager;
        prefs = new Prefs(PreferenceManager.getDefaultSharedPreferences(this));
        gbPrefs = new GBPrefs(prefs);
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public PackageManager getPackageManager() {
        return mPackageManager;
    }

    public Prefs getPrefs() {
        return prefs;
    }

    public GBPrefs getGBPrefs() {
        return gbPrefs;
    }
}

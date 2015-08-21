package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.Context;
import android.content.pm.PackageManager;
import android.test.mock.MockApplication;

import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GBMockApplication extends MockApplication {
    private final PackageManager mPackageManager;

    public GBMockApplication(PackageManager packageManager) {
        GB.environment = GBEnvironment.createDeviceEnvironment().createLocalTestEnvironment();
        mPackageManager = packageManager;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }
    @Override
    public PackageManager getPackageManager() {
        return mPackageManager;
    }

}

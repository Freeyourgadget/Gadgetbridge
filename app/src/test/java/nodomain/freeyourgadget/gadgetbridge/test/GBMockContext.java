package nodomain.freeyourgadget.gadgetbridge.test;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.test.mock.MockContext;

public class GBMockContext extends MockContext {
    private final Application mApplication;

    public GBMockContext(Application application) {
        mApplication = application;
    }

    @Override
    public Context getApplicationContext() {
        return mApplication;
    }

    @Override
    public PackageManager getPackageManager() {
        return mApplication.getPackageManager();
    }
}

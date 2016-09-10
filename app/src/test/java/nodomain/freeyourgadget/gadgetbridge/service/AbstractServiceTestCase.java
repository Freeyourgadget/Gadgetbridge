package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.Application;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;

import org.robolectric.RuntimeEnvironment;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.test.MockHelper;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public abstract class AbstractServiceTestCase<T extends Service> extends TestBase {
    private Context mContext;
    private GBApplication mApplication;
    private NotificationManager mNotificationManager;
    private MockHelper mMockHelper;

    protected AbstractServiceTestCase() {
    }

    public Context getContext() {
        return mContext;
    }

    protected MockHelper getmMockHelper() {
        return mMockHelper;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mMockHelper = new MockHelper();
        mApplication = (GBApplication) RuntimeEnvironment.application;
        mContext = mApplication;
        mNotificationManager = mMockHelper.createNotificationManager(mContext);
    }

    protected Application getApplication() {
        return mApplication;
    }

    private NotificationManager getNotificationService() {
        return mNotificationManager;
    }
}

package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.Application;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

import nodomain.freeyourgadget.gadgetbridge.test.GBMockApplication;
import nodomain.freeyourgadget.gadgetbridge.test.GBMockContext;
import nodomain.freeyourgadget.gadgetbridge.test.GBMockPackageManager;
import nodomain.freeyourgadget.gadgetbridge.test.MockHelper;

public abstract class AbstractServiceTestCase<T extends Service> {
    private static final int ID = -1; // currently not supported
    private final Class<T> mServiceClass;
    private T mServiceInstance;
    private Context mContext;
    private GBMockApplication mApplication;
    private boolean wasStarted;
    private PackageManager mPackageManager;
    private NotificationManager mNotificationManager;
    private MockHelper mMockHelper;

    protected AbstractServiceTestCase(Class<T> serviceClass) {
        mServiceClass = serviceClass;
        Assert.assertNotNull(serviceClass);
    }

    public Context getContext() {
        return mContext;
    }

    public T getServiceInstance() {
        return mServiceInstance;
    }

    protected MockHelper getmMockHelper() {
        return mMockHelper;
    }

    @Before
    public void setUp() throws Exception {
        mMockHelper = new MockHelper();
        mPackageManager = createPackageManager();
        mApplication = createApplication(mPackageManager);
        mContext = createContext(mApplication);
        mNotificationManager = mMockHelper.createNotificationManager(mContext);
        mServiceInstance = createService(mServiceClass, mApplication, mNotificationManager);
        mServiceInstance.onCreate();
    }

    @After
    public void tearDown() throws Exception {
        if (mServiceInstance != null) {
            stopService();
        }
    }

    public void startService(Intent intent) {
        wasStarted = true;
        mServiceInstance.onStartCommand(intent, Service.START_FLAG_REDELIVERY, ID);
    }

    public void stopService() {
        mServiceInstance.onDestroy();
        mServiceInstance = null;
    }

    protected GBMockApplication createApplication(PackageManager packageManager) {
        return new GBMockApplication(packageManager);
    }

    protected PackageManager createPackageManager() {
        return new GBMockPackageManager();
    }

    protected Application getApplication() {
        return mApplication;
    }

    protected Context createContext(final Application application) {
        return new GBMockContext(application);
    }

    protected T createService(Class<T> serviceClass, GBMockApplication application, NotificationManager notificationManager) throws Exception {
        T service = mMockHelper.createService(serviceClass, application);
        mMockHelper.addSystemServiceTo(service, Context.NOTIFICATION_SERVICE, notificationManager);
        return service;
    }

    private NotificationManager getNotificationService() {
        return mNotificationManager;
    }
}

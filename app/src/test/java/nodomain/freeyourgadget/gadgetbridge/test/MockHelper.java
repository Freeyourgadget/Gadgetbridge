package nodomain.freeyourgadget.gadgetbridge.test;

import android.app.Application;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;

import junit.framework.Assert;

import org.mockito.Mockito;

import java.lang.reflect.Constructor;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class MockHelper {
    public <T extends Service> NotificationManager createNotificationManager(Context mContext) throws Exception {
        return (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public <T extends Service> T createService(Class<T> serviceClass, Application application) throws Exception {
        Constructor<T> constructor = serviceClass.getConstructor();
        Assert.assertNotNull(constructor);
        T realService = constructor.newInstance();
        T mockedService = Mockito.spy(realService);
        Mockito.when(mockedService.getApplicationContext()).thenReturn(application);
        Mockito.when(mockedService.getPackageManager()).thenReturn(application.getPackageManager());
        return mockedService;
    }

    public <T extends DeviceCommunicationService> T createDeviceCommunicationService(Class<T> serviceClass, GBApplication application) throws Exception {
        T mockedService = createService(serviceClass, application);
        Mockito.when(mockedService.getPrefs()).thenReturn(GBApplication.getPrefs());
        Mockito.when(mockedService.getGBPrefs()).thenReturn(GBApplication.getGBPrefs());
        return mockedService;
    }

    public void addSystemServiceTo(Context context, String serviceName, Object service) {
        Mockito.when(context.getSystemService(serviceName)).thenReturn(service);
    }
}

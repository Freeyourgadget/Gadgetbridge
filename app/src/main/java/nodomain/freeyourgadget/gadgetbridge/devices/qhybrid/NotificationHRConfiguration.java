package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.util.Log;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public class NotificationHRConfiguration implements Serializable {
    private String packageName;
    private long id = -1;

    public NotificationHRConfiguration(String packageName, long id) {
        this.packageName = packageName;
        this.id = id;
    }

    public String getPackageName() {
        return packageName;
    }

    public long getId() {
        return id;
    }
}

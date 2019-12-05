package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.util.Log;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public class NotificationConfiguration implements Serializable {
    private short min, hour, subEye = -1;
    private String packageName, appName;
    private PlayNotificationRequest.VibrationType vibration;
    private boolean respectSilentMode;
    private long id = -1;

    NotificationConfiguration(short min, short hour, String packageName, String appName, boolean respectSilentMode, PlayNotificationRequest.VibrationType vibration) {
        this.min = min;
        this.hour = hour;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = respectSilentMode;
        this.vibration = vibration;
    }

    public NotificationConfiguration(short min, short hour, short subEye, PlayNotificationRequest.VibrationType vibration) {
        this.min = min;
        this.hour = hour;
        this.subEye = subEye;
        this.vibration = vibration;
    }

    public NotificationConfiguration(short min, short hour, String packageName, String appName, boolean respectSilentMode, PlayNotificationRequest.VibrationType vibration, long id) {
        this.min = min;
        this.hour = hour;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = respectSilentMode;
        this.vibration = vibration;
        this.id = id;
    }
    NotificationConfiguration(String packageName, String appName) {
        this.min = -1;
        this.hour = -1;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = false;
        this.vibration = PlayNotificationRequest.VibrationType.SINGLE_NORMAL;
        this.id = -1;
    }

    public PlayNotificationRequest.VibrationType getVibration() {
        return vibration;
    }

    public void setVibration(PlayNotificationRequest.VibrationType vibration) {
        this.vibration = vibration;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean getRespectSilentMode() {
        Log.d("Config", "respect: " + respectSilentMode);
        return respectSilentMode;
    }

    public void setRespectSilentMode(boolean respectSilentMode) {
        this.respectSilentMode = respectSilentMode;
    }

    public void setMin(short min) {
        this.min = min;
    }

    public void setHour(short hour) {
        this.hour = hour;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public short getMin() {
        return min;
    }

    public short getHour() {
        return hour;
    }

    public short getSubEye() {
        return subEye;
    }

    public void setSubEye(short subEye) {
        this.subEye = subEye;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }
}

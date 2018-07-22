package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.util.Log;

import com.misfit.ble.setting.sam.SAMEnum;

import java.io.Serializable;

public class PackageConfig implements Serializable {
    private int min, hour;
    private String packageName, appName;
    private int vibration;
    private boolean respectSilentMode;
    private long id = -1;

    public PackageConfig(int min, int hour, String packageName, String appName, boolean respectSilentMode, int vibration) {
        this.min = min;
        this.hour = hour;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = respectSilentMode;
        this.vibration = vibration;
    }

    public PackageConfig(int min, int hour, String packageName, String appName, boolean respectSilentMode, int vibration, long id) {
        this.min = min;
        this.hour = hour;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = respectSilentMode;
        this.vibration = vibration;
        this.id = id;
    }
    public PackageConfig(String packageName, String appName) {
        this.min = -1;
        this.hour = -1;
        this.packageName = packageName;
        this.appName = appName;
        this.respectSilentMode = false;
        this.vibration = SAMEnum.VibeEnum.SINGLE_SHORT_VIBE.getId();
        this.id = -1;
    }

    public int getVibration() {
        return vibration;
    }

    public void setVibration(int vibration) {
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

    public void setMin(int min) {
        this.min = min;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getMin() {
        return min;
    }

    public int getHour() {
        return hour;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }
}

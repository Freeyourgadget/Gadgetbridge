package nodomain.freeyourgadget.gadgetbridge.util.preferences;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

import android.content.SharedPreferences;

import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class DevicePrefs extends Prefs {
    public DevicePrefs(final SharedPreferences preferences) {
        super(preferences);
    }

    public boolean getBatteryShowInNotification(final int batteryIndex) {
        return getBoolean(PREF_BATTERY_SHOW_IN_NOTIFICATION + batteryIndex, true);
    }

    public boolean getBatteryNotifyLowEnabled(final BatteryConfig batteryConfig) {
        return getBoolean(PREF_BATTERY_NOTIFY_LOW_ENABLED + batteryConfig.getBatteryIndex(), true);
    }

    public int getBatteryNotifyLowThreshold(final BatteryConfig batteryConfig) {
        return getInt(PREF_BATTERY_NOTIFY_LOW_THRESHOLD + batteryConfig.getBatteryIndex(), batteryConfig.getDefaultLowThreshold());
    }

    public boolean getBatteryNotifyFullEnabled(final BatteryConfig batteryConfig) {
        return getBoolean(PREF_BATTERY_NOTIFY_FULL_ENABLED + batteryConfig.getBatteryIndex(), true);
    }

    public int getBatteryNotifyFullThreshold(final BatteryConfig batteryConfig) {
        return getInt(PREF_BATTERY_NOTIFY_FULL_THRESHOLD + batteryConfig.getBatteryIndex(), batteryConfig.getDefaultFullThreshold());
    }

    public int getBatteryPollingIntervalMinutes() {
        return getInt(PREF_BATTERY_POLLING_INTERVAL, 15);
    }

    public boolean getFetchUnknownFiles() {
        return getBoolean("fetch_unknown_files", false);
    }
}

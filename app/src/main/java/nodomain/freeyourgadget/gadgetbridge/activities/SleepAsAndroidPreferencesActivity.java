package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.devices.SleepAsAndroidFeature;

public class SleepAsAndroidPreferencesActivity extends AbstractSettingsActivityV2 {
    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new SleepAsAndroidPreferencesFragment();
    }

    public static class SleepAsAndroidPreferencesFragment extends AbstractPreferenceFragment {
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.sleepasandroid_preferences, rootKey);

            final ListPreference sleepAsAndroidSlots = findPreference("sleepasandroid_alarm_slot");
            if (sleepAsAndroidSlots != null)
            {
                loadAlarmSlots(sleepAsAndroidSlots);
            }

            final ListPreference sleepAsAndroidDevices = findPreference("sleepasandroid_device");
            if (sleepAsAndroidDevices != null) {
                loadDevicesList(sleepAsAndroidDevices);
                sleepAsAndroidDevices.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                        GBDevice device = GBApplication.app().getDeviceManager().getDeviceByAddress(newValue.toString());
                        if (device != null) {

                            GBApplication.getPrefs().getPreferences().edit().putString("sleepasandroid_device", device.getAddress()).apply();

                            Set<SleepAsAndroidFeature> supportedFeatures = device.getDeviceCoordinator().getSleepAsAndroidFeatures();
                            findPreference("sleepasandroid_alarm_slot").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ALARMS));
                            findPreference("pref_key_sleepasandroid_feat_alarms").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ALARMS));
                            findPreference("pref_key_sleepasandroid_feat_notifications").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.NOTIFICATIONS));
                            findPreference("pref_key_sleepasandroid_feat_movement").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ACCELEROMETER));
                            findPreference("pref_key_sleepasandroid_feat_hr").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.HEART_RATE));
                            findPreference("pref_key_sleepasandroid_feat_oximetry").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.OXIMETRY));
                            findPreference("pref_key_sleepasandroid_feat_spo2").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.SPO2));

                            ListPreference alarmSlots = findPreference("sleepasandroid_alarm_slot");
                            if (alarmSlots != null)
                            {
                                loadAlarmSlots(alarmSlots);
                                if (alarmSlots.getEntries().length > 0)
                                {
                                    alarmSlots.setValueIndex(0);
                                    GB.toast(getString(R.string.alarm_slot_reset), Toast.LENGTH_SHORT, GB.WARN);
                                }
                            }
                        }
                        return false;
                    }
                });

            }

            String defaultDeviceAddr = GBApplication.getPrefs().getString("sleepasandroid_device", "");
            if (!defaultDeviceAddr.isEmpty()) {
                GBDevice device = GBApplication.app().getDeviceManager().getDeviceByAddress(defaultDeviceAddr);
                if (device != null) {

                    Set<SleepAsAndroidFeature> supportedFeatures = device.getDeviceCoordinator().getSleepAsAndroidFeatures();
                    findPreference("sleepasandroid_alarm_slot").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ALARMS));
                    findPreference("pref_key_sleepasandroid_feat_alarms").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ALARMS));
                    findPreference("pref_key_sleepasandroid_feat_notifications").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.NOTIFICATIONS));
                    findPreference("pref_key_sleepasandroid_feat_movement").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ACCELEROMETER));
                    findPreference("pref_key_sleepasandroid_feat_hr").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.HEART_RATE));
                    findPreference("pref_key_sleepasandroid_feat_oximetry").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.OXIMETRY));
                    findPreference("pref_key_sleepasandroid_feat_spo2").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.SPO2));
                }
            }
        }
    }

    private static void loadAlarmSlots(ListPreference sleepAsAndroidSlots) {
        if (sleepAsAndroidSlots != null) {
            String defaultDeviceAddr = GBApplication.getPrefs().getString("sleepasandroid_device", "");
            if (!defaultDeviceAddr.isEmpty()) {
                GBDevice device = GBApplication.app().getDeviceManager().getDeviceByAddress(defaultDeviceAddr);
                if (device != null) {
                    int maxAlarmSlots = device.getDeviceCoordinator().getAlarmSlotCount(device);
                    if (maxAlarmSlots > 0) {
                        List<String> alarmSlots = new ArrayList<>();
                        int reservedAlarmSlots = GBApplication.getPrefs().getInt(DeviceSettingsPreferenceConst.PREF_RESERVER_ALARMS_CALENDAR, 0);
                        for (int i = reservedAlarmSlots + 1;i < maxAlarmSlots; i++) {
                            alarmSlots.add(String.valueOf(i));
                        }
                        sleepAsAndroidSlots.setEntryValues(alarmSlots.toArray(new String[0]));
                        sleepAsAndroidSlots.setEntries(alarmSlots.toArray(new String[0]));
                    }
                }
            }
        }
    }

    private static void loadDevicesList(ListPreference sleepAsAndroidDevices) {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        List<String> deviceMACs = new ArrayList<>();
        List<String> deviceNames = new ArrayList<>();
        for (GBDevice dev : devices) {
            if (dev.getDeviceCoordinator().supportsSleepAsAndroid()) {
                deviceMACs.add(dev.getAddress());
                deviceNames.add(dev.getAliasOrName());
            }
        }

        sleepAsAndroidDevices.setEntryValues(deviceMACs.toArray(new String[0]));
        sleepAsAndroidDevices.setEntries(deviceNames.toArray(new String[0]));
    }
}

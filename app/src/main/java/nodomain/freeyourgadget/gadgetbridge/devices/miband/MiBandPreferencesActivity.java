package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenter;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_GENERIC;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_INCOMING_CALL;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_K9MAIL;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_PEBBLEMSG;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_SMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ADDRESS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_DONT_ACK_TRANSFER;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_FITNESS_GOAL;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_USE_HR_FOR_SLEEP_DETECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_WEARSIDE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_USER_ALIAS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_PROFILE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefKey;

public class MiBandPreferencesActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.miband_preferences);

        final Preference developmentMiaddr = findPreference(PREF_MIBAND_ADDRESS);
        developmentMiaddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                Intent refreshIntent = new Intent(ControlCenter.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });

        final Preference enableHeartrateSleepSupport = findPreference(PREF_MIBAND_USE_HR_FOR_SLEEP_DETECTION);
        enableHeartrateSleepSupport.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onEnableHeartRateSleepSupport(Boolean.TRUE.equals(newVal));
                return true;
            }
        });
    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        return new String[]{
                PREF_USER_ALIAS,
                PREF_MIBAND_ADDRESS,
                PREF_MIBAND_FITNESS_GOAL,
                PREF_MIBAND_DONT_ACK_TRANSFER,
                PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR,
                getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_SMS),
                getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_INCOMING_CALL),
                getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_K9MAIL),
                getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_PEBBLEMSG),
                getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_GENERIC),
        };
    }
}

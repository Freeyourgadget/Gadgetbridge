package nodomain.freeyourgadget.gadgetbridge.miband;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;

import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_GENERIC;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_K9MAIL;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_SMS;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ADDRESS;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_USER_ALIAS;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_USER_GENDER;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_USER_HEIGHT_CM;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_USER_WEIGHT_KG;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_USER_YEAR_OF_BIRTH;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.getNotificationPrefKey;

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

    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        return new String[]{
                PREF_USER_ALIAS,
                PREF_USER_YEAR_OF_BIRTH,
                PREF_USER_GENDER,
                PREF_USER_HEIGHT_CM,
                PREF_USER_WEIGHT_KG,
                PREF_MIBAND_ADDRESS,
                getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_SMS),
                getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_K9MAIL),
                getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_GENERIC),
        };
    }
}

package nodomain.freeyourgadget.gadgetbridge.miband;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;

public class MiBandPreferencesActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.miband_preferences);

        final Preference developmentMiaddr = findPreference(MiBandConst.PREF_MIBAND_ADDRESS);
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
                MiBandConst.PREF_USER_ALIAS,
                MiBandConst.PREF_USER_YEAR_OF_BIRTH,
                MiBandConst.PREF_USER_GENDER,
                MiBandConst.PREF_USER_HEIGHT_CM,
                MiBandConst.PREF_USER_WEIGHT_KG,
                MiBandConst.PREF_MIBAND_ADDRESS
        };
    }
}

package nodomain.freeyourgadget.gadgetbridge.miband;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import nodomain.freeyourgadget.gadgetbridge.GB;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;

public class MiBandPreferencesActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.miband_preferences);
    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        String[] prefsWithSummary = {
                MiBandConst.PREF_USER_ALIAS,
                MiBandConst.PREF_USER_YEAR_OF_BIRTH,
                MiBandConst.PREF_USER_GENDER,
                MiBandConst.PREF_USER_HEIGHT_CM,
                MiBandConst.PREF_USER_WEIGHT_KG
        };
        return prefsWithSummary;
    }
}

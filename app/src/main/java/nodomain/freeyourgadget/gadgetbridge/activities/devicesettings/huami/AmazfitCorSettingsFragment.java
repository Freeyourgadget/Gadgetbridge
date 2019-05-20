package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.huami;

import android.os.Bundle;

import nodomain.freeyourgadget.gadgetbridge.R;

public class AmazfitCorSettingsFragment extends HuamiSettingsFragment{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.devicesettings_huami_cor);
        setChangeListener();
    }

    public static HuamiSettingsFragment newInstance(String settingsFileSuffix) {
        HuamiSettingsFragment fragment = new AmazfitCorSettingsFragment();
        fragment.setSettingsFileSuffix(settingsFileSuffix);

        return fragment;
    }
}

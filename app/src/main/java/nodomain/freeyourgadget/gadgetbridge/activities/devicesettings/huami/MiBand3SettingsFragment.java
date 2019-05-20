package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.huami;

import android.os.Bundle;

import nodomain.freeyourgadget.gadgetbridge.R;

public class MiBand3SettingsFragment extends HuamiSettingsFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.devicesettings_huami_miband3);
        setChangeListener();
    }

    public static HuamiSettingsFragment newInstance(String settingsFileSuffix) {
        HuamiSettingsFragment fragment = new MiBand3SettingsFragment();
        fragment.setSettingsFileSuffix(settingsFileSuffix);

        return fragment;
    }
}

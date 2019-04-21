package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import android.os.Bundle;

import nodomain.freeyourgadget.gadgetbridge.R;

public class HuamiSettingsFragment extends DeviceSpecificSettingsFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.devicesettings_huami, rootKey);
    }

    public static HuamiSettingsFragment newInstance(String settingsFileSuffix) {
        HuamiSettingsFragment fragment = new HuamiSettingsFragment();
        fragment.setSettingsFileSuffix(settingsFileSuffix);

        return fragment;
    }

}



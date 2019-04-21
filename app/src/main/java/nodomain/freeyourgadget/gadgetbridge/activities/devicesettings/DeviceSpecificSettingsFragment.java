package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

abstract public class DeviceSpecificSettingsFragment extends PreferenceFragmentCompat {

    void setSettingsFileSuffix(String settingsFileSuffix) {
        Bundle args = new Bundle();
        args.putString("settingsFileSuffix", settingsFileSuffix);
        setArguments(args);
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        String settingsFileSuffix = getArguments().getString("settingsFileSuffix", "_bug");
        getPreferenceManager().setSharedPreferencesName("devicesettings_" + settingsFileSuffix);
    }
}


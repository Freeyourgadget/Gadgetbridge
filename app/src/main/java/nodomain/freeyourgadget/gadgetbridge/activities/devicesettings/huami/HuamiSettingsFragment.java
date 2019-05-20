package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.huami;

import android.os.Bundle;

import androidx.preference.Preference;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsFragment;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;

public class HuamiSettingsFragment extends DeviceSpecificSettingsFragment {

    /*
     * delayed execution so that the preferences are applied first
     */
    private void invokeLater(Runnable runnable) {
        getListView().post(runnable);
    }

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

    void setChangeListener() {
        final Preference displayItems = findPreference("display_items");
        if (displayItems != null) {
            displayItems.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(HuamiConst.PREF_DISPLAY_ITEMS);
                        }
                    });
                    return true;
                }
            });
        }
    }
}



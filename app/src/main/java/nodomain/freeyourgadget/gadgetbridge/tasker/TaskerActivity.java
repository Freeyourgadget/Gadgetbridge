package nodomain.freeyourgadget.gadgetbridge.tasker;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;

public class TaskerActivity extends AbstractSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        List<String> devices = new ArrayList<>();
        for (GBDevice device : GBApplication.app().getDeviceManager().getDevices()) {
            devices.add(device.getAddress());
        }

        MultiSelectListPreference actives = (MultiSelectListPreference) findPreference(TaskerPrefs.TASKER_ACTIVE_LIST);
        actives.setEntries(devices.toArray(new String[]{}));
        // add activity
        super.onCreate(savedInstanceState);
    }
}

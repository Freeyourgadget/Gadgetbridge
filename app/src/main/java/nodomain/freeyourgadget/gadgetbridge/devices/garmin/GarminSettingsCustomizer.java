package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps.GarminAgpsStatus;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class GarminSettingsCustomizer  implements DeviceSpecificSettingsCustomizer {

    @Override
    public void onPreferenceChange(Preference preference, DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        final Preference prefAgpsUpdateTime = handler.findPreference(DeviceSettingsPreferenceConst.PREF_AGPS_UPDATE_TIME);
        if (prefAgpsUpdateTime != null) {
            final long ts = prefs.getLong(DeviceSettingsPreferenceConst.PREF_AGPS_UPDATE_TIME, 0L);
            if (ts > 0) {
                prefAgpsUpdateTime.setSummary(sdf.format(new Date(ts)));
            } else {
                prefAgpsUpdateTime.setSummary(handler.getContext().getString(R.string.unknown));
            }
        }

        final Preference prefAgpsStatus = handler.findPreference(DeviceSettingsPreferenceConst.PREF_AGPS_STATUS);
        if (prefAgpsStatus != null) {
            final GarminAgpsStatus agpsStatus = GarminAgpsStatus.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_AGPS_STATUS, GarminAgpsStatus.MISSING.name()));
            prefAgpsStatus.setSummary(handler.getContext().getString(agpsStatus.getText()));
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<GarminSettingsCustomizer> CREATOR = new Creator<GarminSettingsCustomizer>() {
        @Override
        public GarminSettingsCustomizer createFromParcel(final Parcel in) {
            return new GarminSettingsCustomizer();
        }

        @Override
        public GarminSettingsCustomizer[] newArray(final int size) {
            return new GarminSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }
}

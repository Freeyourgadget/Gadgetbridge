/*  Copyright (C) 2024 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.os.Parcel;
import android.text.InputType;

import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class MiBandSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private final GBDevice device;

    public MiBandSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {

    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        for (final NotificationType type : NotificationType.values()) {
            String countPrefKey = "mi_vibration_count_" + type.getGenericType();
            handler.setInputTypeFor(countPrefKey, InputType.TYPE_CLASS_NUMBER);
            String tryPrefKey = "mi_try_" + type.getGenericType();
            final Preference tryPref = handler.findPreference(tryPrefKey);
            if (tryPref != null) {
                tryPref.setOnPreferenceClickListener(preference -> {
                    tryVibration(type);
                    return true;
                });
            }
        }
    }

    private void tryVibration(NotificationType type) {
        NotificationSpec spec = new NotificationSpec();
        spec.type = type;
        GBApplication.deviceService(device).onNotification(spec);
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<MiBandSettingsCustomizer> CREATOR = new Creator<MiBandSettingsCustomizer>() {
        @Override
        public MiBandSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(MiBandSettingsCustomizer.class.getClassLoader());
            return new MiBandSettingsCustomizer(device);
        }

        @Override
        public MiBandSettingsCustomizer[] newArray(final int size) {
            return new MiBandSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(device, 0);
    }
}

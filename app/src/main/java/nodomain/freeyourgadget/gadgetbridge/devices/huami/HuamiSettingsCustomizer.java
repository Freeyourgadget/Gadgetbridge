/*  Copyright (C) 2022 José Rebelo

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.os.Parcel;
import android.text.InputType;

import androidx.preference.Preference;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class HuamiSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    final GBDevice device;

    public HuamiSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        // Nothing to do here
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, Prefs prefs) {
        for (HuamiVibrationPatternNotificationType notificationType : HuamiVibrationPatternNotificationType.values()) {
            final String typeKey = notificationType.name().toLowerCase(Locale.ROOT);

            handler.addPreferenceHandlerFor(HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_PREFIX + typeKey);
            handler.addPreferenceHandlerFor(HuamiConst.PREF_HUAMI_VIBRATION_COUNT_PREFIX + typeKey);
            handler.setInputTypeFor(HuamiConst.PREF_HUAMI_VIBRATION_COUNT_PREFIX + typeKey, InputType.TYPE_CLASS_NUMBER);

            final String tryPrefKey = HuamiConst.PREF_HUAMI_VIBRATION_TRY_PREFIX + typeKey;
            final Preference tryPref = handler.findPreference(tryPrefKey);
            if (tryPref != null) {
                tryPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        GBApplication.deviceService().onSendConfiguration(tryPrefKey);
                        return true;
                    }
                });
            }
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        final Set<String> keysWithSummary = new HashSet<>();

        for (HuamiVibrationPatternNotificationType notificationType : HuamiVibrationPatternNotificationType.values()) {
            final String typeKey = notificationType.name().toLowerCase(Locale.ROOT);
            keysWithSummary.add(HuamiConst.PREF_HUAMI_VIBRATION_COUNT_PREFIX + typeKey);
        }

        return keysWithSummary;
    }

    public static final Creator<HuamiSettingsCustomizer> CREATOR = new Creator<HuamiSettingsCustomizer>() {
        @Override
        public HuamiSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(HuamiSettingsCustomizer.class.getClassLoader());
            return new HuamiSettingsCustomizer(device);
        }

        @Override
        public HuamiSettingsCustomizer[] newArray(final int size) {
            return new HuamiSettingsCustomizer[size];
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

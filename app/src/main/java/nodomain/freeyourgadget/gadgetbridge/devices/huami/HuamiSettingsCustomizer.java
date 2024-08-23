/*  Copyright (C) 2022-2024 José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.os.Parcel;
import android.text.InputType;

import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class HuamiSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    final GBDevice device;
    final List<HuamiVibrationPatternNotificationType> vibrationPatternNotificationTypes;

    public HuamiSettingsCustomizer(final GBDevice device,
                                   final List<HuamiVibrationPatternNotificationType> vibrationPatternNotificationTypes) {
        this.device = device;
        this.vibrationPatternNotificationTypes = vibrationPatternNotificationTypes;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        // Nothing to do here
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Preference hrAlertActivePref = handler.findPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_ACTIVE_HIGH_THRESHOLD);
        if (hrAlertActivePref != null) {
            hrAlertActivePref.setVisible(false);
        }

        // Setup the vibration patterns for all supported notification types
        for (HuamiVibrationPatternNotificationType notificationType : HuamiVibrationPatternNotificationType.values()) {
            final String typeKey = notificationType.name().toLowerCase(Locale.ROOT);

            // Hide unsupported notification types
            if (!vibrationPatternNotificationTypes.contains(notificationType)) {
                final String screenKey = HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_KEY_PREFIX + typeKey;
                final Preference pref = handler.findPreference(screenKey);
                if (pref != null) {
                    pref.setVisible(false);
                }
                continue;
            }

            handler.addPreferenceHandlerFor(HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_PREFIX + typeKey);
            handler.addPreferenceHandlerFor(HuamiConst.PREF_HUAMI_VIBRATION_COUNT_PREFIX + typeKey);
            handler.setInputTypeFor(HuamiConst.PREF_HUAMI_VIBRATION_COUNT_PREFIX + typeKey, InputType.TYPE_CLASS_NUMBER);

            // Setup the try pref to vibrate the device
            final String tryPrefKey = HuamiConst.PREF_HUAMI_VIBRATION_TRY_PREFIX + typeKey;
            final Preference tryPref = handler.findPreference(tryPrefKey);
            if (tryPref != null) {
                tryPref.setOnPreferenceClickListener(preference -> {
                    GBApplication.deviceService(device).onSendConfiguration(tryPrefKey);
                    return true;
                });
            }

            // Setup the default preference - disable count if default preference is selected
            final String profilePrefKey = HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_PREFIX + typeKey;
            final String countPrefKey = HuamiConst.PREF_HUAMI_VIBRATION_COUNT_PREFIX + typeKey;
            final Preference countPref = handler.findPreference(countPrefKey);

            final Preference.OnPreferenceChangeListener profilePrefListener = (preference, newValue) -> {
                if (countPref != null) {
                    countPref.setEnabled(!HuamiConst.PREF_HUAMI_DEFAULT_VIBRATION_PROFILE.equals(newValue));
                }
                return true;
            };

            profilePrefListener.onPreferenceChange(null, prefs.getString(profilePrefKey, HuamiConst.PREF_HUAMI_DEFAULT_VIBRATION_PROFILE));
            handler.addPreferenceHandlerFor(profilePrefKey, profilePrefListener);
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return new HashSet<>();
    }

    public static final Creator<HuamiSettingsCustomizer> CREATOR = new Creator<HuamiSettingsCustomizer>() {
        @Override
        public HuamiSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(HuamiSettingsCustomizer.class.getClassLoader());
            final List<HuamiVibrationPatternNotificationType> vibrationPatternNotificationTypes = new ArrayList<>();
            in.readList(vibrationPatternNotificationTypes, HuamiVibrationPatternNotificationType.class.getClassLoader());
            return new HuamiSettingsCustomizer(device, vibrationPatternNotificationTypes);
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
        dest.writeList(vibrationPatternNotificationTypes);
    }
}

/*  Copyright (C) 2024 Jonathan Gobbo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds5pro;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class RedmiBuds5ProSettingsCustomizer implements DeviceSpecificSettingsCustomizer {

    final GBDevice device;

    public RedmiBuds5ProSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }


    @Override
    public void onPreferenceChange(Preference preference, DeviceSpecificSettingsHandler handler) {

        // Hide or Show ANC/Transparency settings according to the current ambient sound control mode
        if (preference.getKey().equals(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_AMBIENT_SOUND_CONTROL)) {
            String mode = ((ListPreference) preference).getValue();
            final Preference ancLevel = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_NOISE_CANCELLING_STRENGTH);
            final Preference transparencyLevel = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_TRANSPARENCY_STRENGTH);
            final Preference adaptiveAnc = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_ADAPTIVE_NOISE_CANCELLING);
//            final Preference customizedAnc = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_PERSONALIZED_NOISE_CANCELLING);
            if (ancLevel != null) {
                ancLevel.setVisible(mode.equals("1"));
            }
            if (transparencyLevel != null) {
                transparencyLevel.setVisible(mode.equals("2"));
            }
            if (adaptiveAnc != null) {
                adaptiveAnc.setVisible(mode.equals("1"));
            }
//            if (customizedAnc != null) {
//                customizedAnc.setVisible(mode.equals("1"));
//            }
        }
    }

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, String rootKey) {

        final ListPreference longPressLeft = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_LEFT);
        final ListPreference longPressRight = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_RIGHT);

        final Preference longPressLeftSettings = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_SETTINGS_LEFT);
        final Preference longPressRightSettings = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_SETTINGS_RIGHT);

        final ListPreference equalizerPreset = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_PRESET);

        if (longPressLeft != null) {
            final Preference.OnPreferenceChangeListener longPressLeftButtonListener = (preference, newVal) -> {
                String mode = newVal.toString();
                if (longPressLeftSettings != null) {
                    longPressLeftSettings.setVisible(mode.equals("6"));
                }
                return true;
            };
            longPressLeftButtonListener.onPreferenceChange(longPressLeft, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_LEFT, "6"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_LEFT, longPressLeftButtonListener);
        }
        if (longPressRight != null) {
            final Preference.OnPreferenceChangeListener longPressRightButtonListener = (preference, newVal) -> {
                String mode = newVal.toString();
                if (longPressRightSettings != null) {
                    longPressRightSettings.setVisible(mode.equals("6"));
                }
                return true;
            };
            longPressRightButtonListener.onPreferenceChange(longPressRight, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_RIGHT, "6"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_RIGHT, longPressRightButtonListener);
        }

        if (equalizerPreset != null) {

            final Preference.OnPreferenceChangeListener equalizerPresetListener = (preference, newVal) -> {

                final List<Preference> prefsToDisable = Arrays.asList(
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k)
                );

                String mode = newVal.toString();
                for (Preference pref : prefsToDisable) {
                    if (pref != null) {
                        pref.setEnabled(mode.equals("10"));
                    }
                }
                return true;
            };
            equalizerPresetListener.onPreferenceChange(equalizerPreset, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_PRESET, "0"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_PRESET, equalizerPresetListener);
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeParcelable(device, 0);
    }

    public static final Creator<RedmiBuds5ProSettingsCustomizer> CREATOR = new Creator<RedmiBuds5ProSettingsCustomizer>() {
        @Override
        public RedmiBuds5ProSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(RedmiBuds5ProSettingsCustomizer.class.getClassLoader());
            return new RedmiBuds5ProSettingsCustomizer(device);
        }

        @Override
        public RedmiBuds5ProSettingsCustomizer[] newArray(final int size) {
            return new RedmiBuds5ProSettingsCustomizer[size];
        }
    };
}

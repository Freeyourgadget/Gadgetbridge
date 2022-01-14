/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_CODEC;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_1000;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_16000;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_2500;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_400;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_6300;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BASS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_SOUND_POSITION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE;

import android.os.Parcel;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class SonyHeadphonesSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    final GBDevice device;

    public SonyHeadphonesSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        // Disable equalizer, sound position and surround mode if not in SBC codec, for WH-1000XM3
        // TODO: Should the coordinator be responsible for this compatibility check?
        if (preference.getKey().equals(PREF_SONY_AUDIO_CODEC) && device.getType().equals(DeviceType.SONY_WH_1000XM3)) {
            final boolean isSbcCodec = ((EditTextPreference) preference).getText().equalsIgnoreCase("sbc");

            final List<Preference> prefsToDisable = Arrays.asList(
                    handler.findPreference(PREF_SONY_EQUALIZER),
                    handler.findPreference(PREF_SONY_EQUALIZER_MODE),
                    handler.findPreference(PREF_SONY_EQUALIZER_BAND_400),
                    handler.findPreference(PREF_SONY_EQUALIZER_BAND_1000),
                    handler.findPreference(PREF_SONY_EQUALIZER_BAND_2500),
                    handler.findPreference(PREF_SONY_EQUALIZER_BAND_6300),
                    handler.findPreference(PREF_SONY_EQUALIZER_BAND_16000),
                    handler.findPreference(PREF_SONY_EQUALIZER_BASS),
                    handler.findPreference(PREF_SONY_SOUND_POSITION),
                    handler.findPreference(PREF_SONY_SURROUND_MODE)
            );

            for (Preference pref : prefsToDisable) {
                if (pref != null) {
                    pref.setEnabled(isSbcCodec);
                }
            }
        }
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler) {
        // Only enable the focus on voice check and voice level slider if the ambient sound control mode is ambient sound

        final ListPreference ambientSoundControl = handler.findPreference(PREF_SONY_AMBIENT_SOUND_CONTROL);
        if (ambientSoundControl != null) {
            final Preference focusOnVoice = handler.findPreference(PREF_SONY_FOCUS_VOICE);
            final Preference ambientSoundLevel = handler.findPreference(PREF_SONY_AMBIENT_SOUND_LEVEL);

            final Preference.OnPreferenceChangeListener ambientSoundControlPrefListener = new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    boolean isAmbientSoundEnabled = AmbientSoundControl.Mode.AMBIENT_SOUND.name().toLowerCase(Locale.getDefault()).equals(newVal);
                    focusOnVoice.setEnabled(isAmbientSoundEnabled);
                    ambientSoundLevel.setEnabled(isAmbientSoundEnabled);

                    return true;
                }
            };

            ambientSoundControlPrefListener.onPreferenceChange(ambientSoundControl, ambientSoundControl.getValue());
            handler.addPreferenceHandlerFor(PREF_SONY_AMBIENT_SOUND_CONTROL, ambientSoundControlPrefListener);
        }
    }

    public static final Creator<SonyHeadphonesSettingsCustomizer> CREATOR = new Creator<SonyHeadphonesSettingsCustomizer>() {
        @Override
        public SonyHeadphonesSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(SonyHeadphonesSettingsCustomizer.class.getClassLoader());
            return new SonyHeadphonesSettingsCustomizer(device);
        }

        @Override
        public SonyHeadphonesSettingsCustomizer[] newArray(final int size) {
            return new SonyHeadphonesSettingsCustomizer[size];
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

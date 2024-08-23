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
package nodomain.freeyourgadget.gadgetbridge.devices.zetime;

import android.os.Parcel;
import android.text.InputType;

import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ZeTimeSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private final GBDevice device;

    public ZeTimeSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {

    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        if (rootKey == null) {
            // Main screen - read the settings
            GBApplication.deviceService(device).onReadConfiguration("do_it");
        }

        // Date time
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_DATE_FORMAT);

        // Display
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_SCREENTIME);
        handler.setInputTypeFor(ZeTimeConstants.PREF_SCREENTIME, InputType.TYPE_CLASS_NUMBER);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_ANALOG_MODE);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_HANDMOVE_DISPLAY);

        // Vibration patterns
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_SMS_SIGNALING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_ANTI_LOSS_SIGNALING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_CALENDAR_SIGNALING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_CALL_SIGNALING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_MISSED_CALL_SIGNALING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_EMAIL_SIGNALING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_INACTIVITY_SIGNALING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_LOW_POWER_SIGNALING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_SOCIAL_SIGNALING);

        // Heart rate
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_HEARTRATE_ALARM);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_MAX_HEARTRATE);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_MIN_HEARTRATE);
        handler.setInputTypeFor(ZeTimeConstants.PREF_ZETIME_MAX_HEARTRATE, InputType.TYPE_CLASS_NUMBER);
        handler.setInputTypeFor(ZeTimeConstants.PREF_ZETIME_MIN_HEARTRATE, InputType.TYPE_CLASS_NUMBER);

        // Activity info
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_ACTIVITY_TRACKING);
        handler.addPreferenceHandlerFor(ZeTimeConstants.PREF_CALORIES_TYPE);
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<ZeTimeSettingsCustomizer> CREATOR = new Creator<ZeTimeSettingsCustomizer>() {
        @Override
        public ZeTimeSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(ZeTimeSettingsCustomizer.class.getClassLoader());
            return new ZeTimeSettingsCustomizer(device);
        }

        @Override
        public ZeTimeSettingsCustomizer[] newArray(final int size) {
            return new ZeTimeSettingsCustomizer[size];
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

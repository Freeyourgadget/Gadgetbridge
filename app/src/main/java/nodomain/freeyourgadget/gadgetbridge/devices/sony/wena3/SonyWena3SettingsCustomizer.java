/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3;

import android.os.Parcel;

import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SonyWena3SettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, final String rootKey) {
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.RICH_DESIGN_MODE);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.LARGE_FONT_SIZE);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.WEATHER_IN_STATUSBAR);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.SMART_VIBRATION);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.SMART_WAKEUP_MARGIN_MINUTES);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.VIBRATION_STRENGTH);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.LEFT_HOME_ICON);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.CENTER_HOME_ICON);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.RIGHT_HOME_ICON);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.DAY_START_HOUR);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_KIND);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_START_HHMM);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_END_HHMM);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.BUTTON_DOUBLE_PRESS_ACTION);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.BUTTON_LONG_PRESS_ACTION);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.MENU_ICON_CSV_KEY);
        handler.addPreferenceHandlerFor(SonyWena3SettingKeys.STATUS_PAGE_CSV_KEY);
    }

    @Override
    public void onPreferenceChange(Preference preference, DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }
    public static final Creator<SonyWena3SettingsCustomizer> CREATOR = new Creator<SonyWena3SettingsCustomizer>() {
        @Override
        public SonyWena3SettingsCustomizer createFromParcel(final Parcel in) {
            return new SonyWena3SettingsCustomizer();
        }

        @Override
        public SonyWena3SettingsCustomizer[] newArray(final int size) {
            return new SonyWena3SettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
    }
}

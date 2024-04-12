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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import androidx.annotation.XmlRes;

import nodomain.freeyourgadget.gadgetbridge.R;

public enum DeviceSpecificSettingsScreen {
    ACTIVITY_INFO("pref_screen_activity_info", R.xml.devicesettings_root_activity_info),
    AUDIO("pref_screen_audio", R.xml.devicesettings_root_audio),
    AUTHENTICATION("pref_screen_authentication", R.xml.devicesettings_root_authentication),
    CALENDAR("pref_screen_calendar", R.xml.devicesettings_root_calendar),
    CALLS_AND_NOTIFICATIONS("pref_screen_calls_and_notifications", R.xml.devicesettings_root_calls_and_notifications),
    CONNECTION("pref_screen_connection", R.xml.devicesettings_root_connection),
    DEVELOPER("pref_screen_developer", R.xml.devicesettings_root_developer),
    DISPLAY("pref_screen_display", R.xml.devicesettings_root_display),
    GENERIC("pref_screen_generic", R.xml.devicesettings_root_generic),
    NOTIFICATIONS("pref_screen_notifications", R.xml.devicesettings_root_notifications),
    DATE_TIME("pref_screen_date_time", R.xml.devicesettings_root_date_time),
    WORKOUT("pref_screen_workout", R.xml.devicesettings_root_workout),
    HEALTH("pref_screen_health", R.xml.devicesettings_root_health),
    ;

    private final String key;
    @XmlRes
    private final int xml;

    DeviceSpecificSettingsScreen(final String key, final int xml) {
        this.key = key;
        this.xml = xml;
    }

    public String getKey() {
        return key;
    }

    public int getXml() {
        return xml;
    }
}

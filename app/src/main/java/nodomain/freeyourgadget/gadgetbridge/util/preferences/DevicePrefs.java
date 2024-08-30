/*  Copyright (C) 2016-2024 Andreas Shimokawa, Anemograph, Carsten Pfeiffer,
    Daniel Dakhno, Daniele Gobbetti, Davis Mosenkovs, Dikay900, Felix Konstantin
    Maurer, José Rebelo, Petr Vaněk, Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.util.preferences;

import java.util.Locale;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

import android.content.SharedPreferences;
import android.text.format.DateFormat;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;

public class DevicePrefs extends Prefs {
    private GBDevice gbDevice;

    public DevicePrefs(final SharedPreferences preferences, GBDevice gbDevice) {
        super(preferences);
        this.gbDevice = gbDevice;
    }

    public boolean getBatteryShowInNotification(final int batteryIndex) {
        return getBoolean(PREF_BATTERY_SHOW_IN_NOTIFICATION + batteryIndex, true);
    }

    public boolean getBatteryNotifyLowEnabled(final BatteryConfig batteryConfig) {
        return getBoolean(PREF_BATTERY_NOTIFY_LOW_ENABLED + batteryConfig.getBatteryIndex(), true);
    }

    public int getBatteryNotifyLowThreshold(final BatteryConfig batteryConfig) {
        return getInt(PREF_BATTERY_NOTIFY_LOW_THRESHOLD + batteryConfig.getBatteryIndex(), batteryConfig.getDefaultLowThreshold());
    }

    public boolean getBatteryNotifyFullEnabled(final BatteryConfig batteryConfig) {
        return getBoolean(PREF_BATTERY_NOTIFY_FULL_ENABLED + batteryConfig.getBatteryIndex(), true);
    }

    public int getBatteryNotifyFullThreshold(final BatteryConfig batteryConfig) {
        return getInt(PREF_BATTERY_NOTIFY_FULL_THRESHOLD + batteryConfig.getBatteryIndex(), batteryConfig.getDefaultFullThreshold());
    }

    public boolean getBatteryPollingEnabled() {
        return getBoolean(PREF_BATTERY_POLLING_ENABLE, true);
    }

    public int getBatteryPollingIntervalMinutes() {
        return getInt(PREF_BATTERY_POLLING_INTERVAL, 15);
    }

    public boolean getFetchUnknownFiles() {
        return getBoolean("fetch_unknown_files", false);
    }

    public String getTimeFormat() {
        String timeFormat = getString(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO);
        if (DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO.equals(timeFormat)) {
            if (DateFormat.is24HourFormat(GBApplication.getContext())) {
                timeFormat = DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H;
            } else {
                timeFormat = DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H;
            }
        }

        return timeFormat;
    }

    public String getDateFormatDayMonthOrder() {
        String dateFormat = getString(DeviceSettingsPreferenceConst.PREF_DATEFORMAT, DeviceSettingsPreferenceConst.PREF_DATEFORMAT_AUTO);
        if (DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO.equals(dateFormat)) {
            String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dM");
            boolean quoted = false;
            for (char c: pattern.toCharArray()) {
                if (c == '\'') {
                    quoted = !quoted;
                    continue;
                }
                if (quoted)
                    continue;
                if (c == 'd')
                    return DeviceSettingsPreferenceConst.PREF_DATEFORMAT_DAY_MONTH;
                if (c == 'M' || c == 'L')
                    return DeviceSettingsPreferenceConst.PREF_DATEFORMAT_MONTH_DAY;
            }
            return DeviceSettingsPreferenceConst.PREF_DATEFORMAT_DAY_MONTH;
        }

        return dateFormat;
    }

    public int getReservedReminderCalendarSlots() {
        if (!gbDevice.getDeviceCoordinator().getReserveReminderSlotsForCalendar())
            return 0;
        if (!getBoolean(DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR, false))
            return 0;
        return getInt(DeviceSettingsPreferenceConst.PREF_RESERVE_REMINDERS_CALENDAR, 9);
    }

}

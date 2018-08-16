/*  Copyright (C) 2016-2018 Carsten Pfeiffer, Daniele Gobbetti, Felix
    Konstantin Maurer

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
package nodomain.freeyourgadget.gadgetbridge.util;

import java.text.ParseException;
import java.util.Date;

public class GBPrefs {
    public static final String PACKAGE_BLACKLIST = "package_blacklist";
    public static final String PACKAGE_PEBBLEMSG_BLACKLIST = "package_pebblemsg_blacklist";
    public static final String CALENDAR_BLACKLIST = "calendar_blacklist";
    public static final String AUTO_RECONNECT = "general_autocreconnect";
    private static final String AUTO_START = "general_autostartonboot";
    public static final String AUTO_EXPORT_ENABLED = "auto_export_enabled";
    public static final String AUTO_EXPORT_LOCATION = "auto_export_location";
    public static final String AUTO_EXPORT_INTERVAL = "auto_export_interval";
    private static final boolean AUTO_START_DEFAULT = true;
    private static final String BG_JS_ENABLED = "pebble_enable_background_javascript";
    private static final boolean BG_JS_ENABLED_DEFAULT = false;
    public static boolean AUTO_RECONNECT_DEFAULT = true;

    public static final String USER_NAME = "mi_user_alias";
    public static final String USER_NAME_DEFAULT = "gadgetbridge-user";
    private static final String USER_BIRTHDAY = "";

    public static final String CHART_MAX_HEART_RATE = "chart_max_heart_rate";
    public static final String CHART_MIN_HEART_RATE = "chart_min_heart_rate";

    private final Prefs mPrefs;

    public GBPrefs(Prefs prefs) {
        mPrefs = prefs;
    }

    public boolean getAutoReconnect() {
        return mPrefs.getBoolean(AUTO_RECONNECT, AUTO_RECONNECT_DEFAULT);
    }

    public boolean getAutoStart() {
        return mPrefs.getBoolean(AUTO_START, AUTO_START_DEFAULT);
    }

    public boolean isBackgroundJsEnabled() {
        return mPrefs.getBoolean(BG_JS_ENABLED, BG_JS_ENABLED_DEFAULT);
    }

    public String getUserName() {
        return mPrefs.getString(USER_NAME, USER_NAME_DEFAULT);
    }

    public Date getUserBirthday() {
        String date = mPrefs.getString(USER_BIRTHDAY, null);
        if (date == null) {
            return null;
        }
        try {
            return DateTimeUtils.dayFromString(date);
        } catch (ParseException ex) {
            GB.log("Error parsing date: " + date, GB.ERROR, ex);
            return null;
        }
    }

    public int getUserGender() {
        return 0;
    }
}

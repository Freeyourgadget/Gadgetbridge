/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public final class XiaomiPreferences {
    public static final String PREF_ALARM_SLOTS = "alarm_slots";
    public static final String PREF_REMINDER_SLOTS = "reminder_slots";
    public static final String PREF_CANNED_MESSAGES_MIN = "canned_messages_min";
    public static final String PREF_CANNED_MESSAGES_MAX = "canned_messages_max";

    public static final String FEAT_DEVICE_ACTIONS = "feat_device_actions";
    public static final String FEAT_DISPLAY_ITEMS = "feat_display_items";
    public static final String FEAT_STRESS = "feat_stress";
    public static final String FEAT_SPO2 = "feat_spo2";
    public static final String FEAT_PASSWORD = "feat_password";
    public static final String FEAT_INACTIVITY = "feat_inactivity";
    public static final String FEAT_SLEEP_MODE_SCHEDULE = "feat_sleep_mode_schedule";
    public static final String FEAT_GOAL_NOTIFICATION = "feat_goal_notification";
    public static final String FEAT_VITALITY_SCORE = "feat_vitality_score";
    public static final String FEAT_SCREEN_ON_ON_NOTIFICATIONS = "feat_screen_on_on_notifications";
    public static final String FEAT_CAMERA_REMOTE = "feat_camera_remote";

    private XiaomiPreferences() {
        // util class
    }

    public static String prefFromHourMin(final XiaomiProto.HourMinute hourMinute) {
        return String.format(Locale.ROOT, "%02d:%02d", hourMinute.getHour(), hourMinute.getMinute());
    }

    public static XiaomiProto.HourMinute prefToHourMin(final Date date) {
        final Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);

        return XiaomiProto.HourMinute.newBuilder()
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build();
    }

    public static Date toDate(final XiaomiProto.Date date, final XiaomiProto.Time time) {
        // For some reason, the watch expects those in UTC...
        // TODO double-check with official app, this does not make sense
        final Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(
                date.getYear(), date.getMonth() - 1, date.getDay(),
                time.getHour(), time.getMinute(), time.getSecond()
        );

        return calendar.getTime();
    }

    public static boolean keepActivityDataOnDevice(final GBDevice gbDevice) {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        return prefs.getBoolean("keep_activity_data_on_device", false);
    }
}

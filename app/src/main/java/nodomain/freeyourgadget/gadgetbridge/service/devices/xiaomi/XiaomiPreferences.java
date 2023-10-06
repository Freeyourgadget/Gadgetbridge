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

import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;

public final class XiaomiPreferences {
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

    /**
     * Returns the preference key where to save the list of possible value for a preference, comma-separated.
     */
    public static String getPrefPossibleValuesKey(final String key) {
        return String.format(Locale.ROOT, "%s_possible_values", key);
    }
}

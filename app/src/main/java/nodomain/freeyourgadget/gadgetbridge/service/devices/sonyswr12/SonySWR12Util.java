/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SonySWR12Util {

    public static long secSince2013() {
        //sony uses time on band since 2013 for some reason
        final Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        instance.set(2013, 0, 1, 0, 0, 0);
        instance.set(14, 0);
        return instance.getTimeInMillis()/1000;
    }

    public static String timeToString(long sec) {
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss");
        return format.format(new Date(sec * 1000));
    }
}

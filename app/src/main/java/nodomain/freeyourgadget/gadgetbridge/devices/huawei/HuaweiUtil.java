/*  Copyright (C) 2024 Damien Gaignon, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuaweiUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiUtil.class);

    public static byte[] timeToByte(String time) {
        Calendar calendar = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        try {
            Date t = df.parse(time);
            assert t != null;
            calendar.setTime(t);
        } catch (ParseException e) {
            LOG.error("Time conversion error: " + e);
            return null;
        }
        return new byte[]{
            (byte)calendar.get(Calendar.HOUR_OF_DAY),
            (byte)calendar.get(Calendar.MINUTE)};
    }

    public static byte[] getTimeAndZoneId(final Calendar now) {
        int zoneRawOffset = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET)) / 1000;
        byte[] id = now.getTimeZone().getID().getBytes();
        return ByteBuffer.allocate(6 + id.length)
            .putInt((int)(now.getTimeInMillis() / 1000))
            .put((byte)(zoneRawOffset < 0 ? (-zoneRawOffset / 3600 + 128) : zoneRawOffset / 3600) )
            .put((byte)(zoneRawOffset / 60 % 60))
            .put(id)
            .array();
    }
}

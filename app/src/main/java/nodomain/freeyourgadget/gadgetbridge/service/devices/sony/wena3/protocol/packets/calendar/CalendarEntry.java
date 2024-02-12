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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.calendar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.util.TimeUtil;

public class CalendarEntry implements Wena3Packetable {
    public final Date begin;
    public final Date end;
    public final boolean isAllDay;
    public final String title;
    public final String location;
    /// 1-based, not index!
    public final byte position;
    public final byte totalItemCount;

    public CalendarEntry(Date begin, Date end, boolean isAllDay, String title, String location, byte position, byte totalItemCount) {
        this.begin = begin;
        this.end = end;
        this.isAllDay = isAllDay;
        this.title = title;
        this.location = location;
        this.position = position;
        this.totalItemCount = totalItemCount;
    }

    @Override
    public byte[] toByteArray() {
        byte[] cstrTitle = title.getBytes(StandardCharsets.UTF_8);
        byte[] cstrLocation = location.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(14 + cstrTitle.length + cstrLocation.length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x02)
                .put(position)
                .put(totalItemCount)
                .put((byte) cstrTitle.length)
                .put((byte) cstrLocation.length)
                .put((byte) (isAllDay ? 0x1 : 0x0))
                .putInt(TimeUtil.dateToWenaTime(begin))
                .putInt(TimeUtil.dateToWenaTime(end))
                .put(cstrTitle)
                .put(cstrLocation)
                .array();

    }

    public static byte[] byteArrayForEmptyEvent(byte position, byte totalItemCount) {
        return ByteBuffer.allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x02)
                .put(position)
                .put(totalItemCount)
                .put((byte) 0x0)
                .put((byte) 0x0)
                .put((byte) 0x0)
                .putInt(0)
                .putInt(0)
                .array();

    }
}

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;

public class TimeZoneSetting implements Wena3Packetable {
    public final TimeZone timeZone;
    public final Date referenceDate;

    public TimeZoneSetting(TimeZone tz, Date referenceDate) {
        this.timeZone = tz;
        this.referenceDate = referenceDate;
    }

    @Override
    public byte[] toByteArray() {
        int offset = timeZone.getOffset(referenceDate.getTime());
        return ByteBuffer
                .allocate(3)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte)0x11)
                .put((byte) (offset / 3_600_000))
                .put((byte) ((offset / 60_000) % 60))
                .array();
    }
}

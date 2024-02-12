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

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.util.TimeUtil;

public class TimeSetting implements Wena3Packetable {
    public final Date currentTime;

    public TimeSetting(Date dateTime) {
        this.currentTime = dateTime;
    }

    @Override
    public byte[] toByteArray() {
        return ByteBuffer.allocate(5)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x10)
                .putInt(TimeUtil.dateToWenaTime(currentTime))
                .array();
    }
}

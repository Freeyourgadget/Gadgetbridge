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

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;

public class DoNotDisturbSettings implements Wena3Packetable {
    public final boolean enable;
    public final int startHour;
    public final int startMinute;
    public final int endHour;
    public final int endMinute;

    public DoNotDisturbSettings(boolean enable, int startHour, int startMinute, int endHour, int endMinute) {
        this.enable = enable;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
    }

    @Override
    public byte[] toByteArray() {
        return ByteBuffer.allocate(6)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x14)
                .put((byte) (enable ? 0x1 : 0x0))
                .put((byte) startHour)
                .put((byte) startMinute)
                .put((byte) endHour)
                .put((byte) endMinute)
                .array();
    }
}

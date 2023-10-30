/*  Copyright (C) 2023 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.NotificationKind;

public class NotificationRemoval implements Wena3Packetable {
    public final NotificationKind kind;
    public final int id;

    public NotificationRemoval(NotificationKind kind, int id) {
        this.kind = kind;
        this.id = id;
    }

    @Override
    public byte[] toByteArray() {
        return ByteBuffer
                .allocate(7)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x01)
                .put((byte)kind.ordinal())
                .putInt(id)
                .array();
    }
}

/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.GarminMessageType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import java.util.HashSet;
import java.util.Set;

public class SyncRequestMessage {
    public static final int OPTION_MANUAL = 0;
    public static final int OPTION_INVISIBLE = 1;
    public static final int OPTION_VISIBLE_AS_NEEDED = 2;

    public final int option;
    public final Set<GarminMessageType> fileTypes;

    public SyncRequestMessage(int option, Set<GarminMessageType> fileTypes) {
        this.option = option;
        this.fileTypes = fileTypes;
    }

    public static SyncRequestMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int option = reader.readByte();
        final int bitMaskSize = reader.readByte();
        final byte[] longBits = reader.readBytesTo(bitMaskSize, new byte[8], 0);
        long bitMask = BLETypeConversions.toUint64(longBits, 0);

        final Set<GarminMessageType> fileTypes = new HashSet<>(GarminMessageType.values().length);
        for (GarminMessageType messageType : GarminMessageType.values()) {
            int num = messageType.ordinal();
            long mask = 1L << num;
            if ((bitMask & mask) != 0) {
                fileTypes.add(messageType);
                bitMask &= ~mask;
            }
        }
        if (bitMask != 0) {
            throw new IllegalArgumentException("Unknown bit mask " + GB.hexdump(longBits, 0, longBits.length));
        }

        return new SyncRequestMessage(option, fileTypes);
    }
}

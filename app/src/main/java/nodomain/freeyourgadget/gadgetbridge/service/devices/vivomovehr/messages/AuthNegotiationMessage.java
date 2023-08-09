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

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class AuthNegotiationMessage {
    public static final int LONG_TERM_KEY_AVAILABILITY_NONE = 0;
    public static final int LONG_TERM_KEY_AVAILABILITY_SOME_AVAILABLE = 1;

    public static final int ENCRYPTION_ALGORITHM_NONE = 0;
    public static final int ENCRYPTION_ALGORITHM_XXTEA = 1 << 0;
    public static final int ENCRYPTION_ALGORITHM_AES128 = 1 << 1;

    public final byte[] packet;

    public AuthNegotiationMessage(int longTermKeyAvailability, int supportedEncryptionAlgorithms) {
        final MessageWriter writer = new MessageWriter(11);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_AUTH_NEGOTIATION);
        writer.writeByte(longTermKeyAvailability);
        writer.writeInt(supportedEncryptionAlgorithms);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

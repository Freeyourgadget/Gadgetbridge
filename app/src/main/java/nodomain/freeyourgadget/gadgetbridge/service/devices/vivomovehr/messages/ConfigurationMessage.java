/*  Copyright (C) 2023-2024 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

import java.util.Arrays;

public class ConfigurationMessage {
    public final byte[] packet;
    public final byte[] configurationPayload;

    public ConfigurationMessage(byte[] configurationPayload) {
        if (configurationPayload.length > 255) throw new IllegalArgumentException("Too long payload");
        this.configurationPayload = configurationPayload;

        final MessageWriter writer = new MessageWriter(7 + configurationPayload.length);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_CONFIGURATION);
        writer.writeByte(configurationPayload.length);
        writer.writeBytes(configurationPayload);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }

    public static ConfigurationMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int payloadSize = reader.readByte();
        return new ConfigurationMessage(Arrays.copyOfRange(packet, 5, payloadSize));
    }
}

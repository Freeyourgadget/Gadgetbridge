package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class CurrentTimeRequestResponseMessage {
    public final byte[] packet;

    public CurrentTimeRequestResponseMessage(int status, int referenceID, int garminTimestamp, int timeZoneOffset, int dstOffset) {
        final MessageWriter writer = new MessageWriter(29);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_RESPONSE);
        writer.writeShort(VivomoveConstants.MESSAGE_CURRENT_TIME_REQUEST);
        writer.writeByte(status);
        writer.writeInt(referenceID);
        writer.writeInt(garminTimestamp);
        writer.writeInt(timeZoneOffset);
        // TODO: next DST start/end
        writer.writeInt(0);
        writer.writeInt(0);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

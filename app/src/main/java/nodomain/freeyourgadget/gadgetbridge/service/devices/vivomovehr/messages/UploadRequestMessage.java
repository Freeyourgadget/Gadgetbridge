package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class UploadRequestMessage {
    public final byte[] packet;

    public UploadRequestMessage(int fileIndex, int dataOffset, int maxSize, int crcSeed) {
        final MessageWriter writer = new MessageWriter(18);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_UPLOAD_REQUEST);
        writer.writeShort(fileIndex);
        writer.writeInt(maxSize);
        writer.writeInt(dataOffset);
        writer.writeShort(crcSeed);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

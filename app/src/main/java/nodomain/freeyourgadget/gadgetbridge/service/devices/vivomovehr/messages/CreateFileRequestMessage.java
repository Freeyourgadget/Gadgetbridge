package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class CreateFileRequestMessage {
    public final byte[] packet;

    public CreateFileRequestMessage(int fileSize, int dataType, int subType, int fileIdentifier, int subTypeMask, int numberMask, String path) {
        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_CREATE_FILE_REQUEST);
        writer.writeInt(fileSize);
        writer.writeByte(dataType);
        writer.writeByte(subType);
        writer.writeShort(fileIdentifier);
        writer.writeByte(0); // reserved
        writer.writeByte(subTypeMask);
        writer.writeShort(numberMask);
        writer.writeString(path);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

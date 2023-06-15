package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class FileTransferDataMessage {
    public final int flags;
    public final int crc;
    public final int dataOffset;
    public final byte[] data;

    public final byte[] packet;

    public FileTransferDataMessage(int flags, int crc, int dataOffset, byte[] data) {
        this.flags = flags;
        this.crc = crc;
        this.dataOffset = dataOffset;
        this.data = data;

        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_FILE_TRANSFER_DATA);
        writer.writeByte(flags);
        writer.writeShort(crc);
        writer.writeInt(dataOffset);
        writer.writeBytes(data);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }

    public static FileTransferDataMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);

        final int flags = reader.readByte();
        final int crc = reader.readShort();
        final int dataOffset = reader.readInt();
        final int dataSize = packet.length - 13;
        final byte[] data = reader.readBytes(dataSize);

        return new FileTransferDataMessage(flags, crc, dataOffset, data);
    }
}

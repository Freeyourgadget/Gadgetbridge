package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class DownloadRequestMessage {
    public static final int REQUEST_CONTINUE_TRANSFER = 0;
    public static final int REQUEST_NEW_TRANSFER = 1;

    public final byte[] packet;

    public DownloadRequestMessage(int fileIndex, int dataOffset, int request, int crcSeed, int dataSize) {
        final MessageWriter writer = new MessageWriter(19);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_DOWNLOAD_REQUEST);
        writer.writeShort(fileIndex);
        writer.writeInt(dataOffset);
        writer.writeByte(request);
        writer.writeShort(crcSeed);
        writer.writeInt(dataSize);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

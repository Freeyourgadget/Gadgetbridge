package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class DirectoryFileFilterRequestMessage {
    public static final int FILTER_NO_FILTER = 0;
    public static final int FILTER_DEVICE_DEFAULT_FILTER = 1;
    public static final int FILTER_CUSTOM_FILTER = 2;
    public static final int FILTER_PENDING_UPLOADS_ONLY = 3;

    public final byte[] packet;

    public DirectoryFileFilterRequestMessage(int filterType) {
        final MessageWriter writer = new MessageWriter(7);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_DIRECTORY_FILE_FILTER_REQUEST);
        writer.writeByte(filterType);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

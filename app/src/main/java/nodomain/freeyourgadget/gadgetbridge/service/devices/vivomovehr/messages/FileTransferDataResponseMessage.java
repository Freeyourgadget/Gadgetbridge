package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class FileTransferDataResponseMessage {
    public static final byte RESPONSE_TRANSFER_SUCCESSFUL = 0;
    public static final byte RESPONSE_RESEND_LAST_DATA_PACKET = 1;
    public static final byte RESPONSE_ABORT_DOWNLOAD_REQUEST = 2;
    public static final byte RESPONSE_ERROR_CRC_MISMATCH = 3;
    public static final byte RESPONSE_ERROR_DATA_OFFSET_MISMATCH = 4;
    public static final byte RESPONSE_SILENT_SYNC_PAUSED = 5;

    public final int status;
    public final int response;
    public final int nextDataOffset;

    public final byte[] packet;

    public FileTransferDataResponseMessage(int status, int response, int nextDataOffset) {
        this.status = status;
        this.response = response;
        this.nextDataOffset = nextDataOffset;

        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_RESPONSE);
        writer.writeShort(VivomoveConstants.MESSAGE_FILE_TRANSFER_DATA);
        writer.writeByte(status);
        writer.writeByte(response);
        writer.writeInt(nextDataOffset);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }

    public static FileTransferDataResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 6);
        final int status = reader.readByte();
        final int response = reader.readByte();
        final int nextDataOffset = reader.readInt();

        return new FileTransferDataResponseMessage(status, response, nextDataOffset);
    }
}

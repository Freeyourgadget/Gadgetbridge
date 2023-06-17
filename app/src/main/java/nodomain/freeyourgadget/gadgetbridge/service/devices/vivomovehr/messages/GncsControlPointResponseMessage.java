package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class GncsControlPointResponseMessage {
    public static final int RESPONSE_SUCCESSFUL = 0;
    public static final int RESPONSE_ANCS_ERROR_OCCURRED = 1;
    public static final int RESPONSE_INVALID_PARAMETERS = 2;

    public static final int ANCS_ERROR_NO_ERROR = 0;
    public static final int ANCS_ERROR_UNKNOWN_ANCS_COMMAND = 0xA0;
    public static final int ANCS_ERROR_INVALID_ANCS_COMMAND = 0xA1;
    public static final int ANCS_ERROR_INVALID_ANCS_PARAMETER = 0xA2;
    public static final int ANCS_ERROR_ACTION_FAILED = 0xA3;

    public final byte[] packet;

    public GncsControlPointResponseMessage(int status, int response, int ancsError) {
        final MessageWriter writer = new MessageWriter(11);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_RESPONSE);
        writer.writeShort(VivomoveConstants.MESSAGE_GNCS_CONTROL_POINT_REQUEST);
        writer.writeByte(status);
        writer.writeByte(response);
        writer.writeByte(ancsError);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

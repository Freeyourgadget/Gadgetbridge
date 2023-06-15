package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;

import java.util.Locale;

public class ResponseMessage {
    public final int requestID;
    public final int status;

    public ResponseMessage(int requestID, int status) {
        this.requestID = requestID;
        this.status = status;
    }

    public static ResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();

        return new ResponseMessage(requestID, status);
    }

    public String getStatusStr() {
        switch (status) {
            case VivomoveConstants.STATUS_ACK:
                return "ACK";
            case VivomoveConstants.STATUS_NAK:
                return "NAK";
            case VivomoveConstants.STATUS_UNSUPPORTED:
                return "UNSUPPORTED";
            case VivomoveConstants.STATUS_DECODE_ERROR:
                return "DECODE ERROR";
            case VivomoveConstants.STATUS_CRC_ERROR:
                return "CRC ERROR";
            case VivomoveConstants.STATUS_LENGTH_ERROR:
                return "LENGTH ERROR";
            default:
                return String.format(Locale.ROOT, "Unknown status %x", status);
        }
    }
}

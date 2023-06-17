package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class NotificationServiceSubscriptionResponseMessage {
    public final byte[] packet;

    public NotificationServiceSubscriptionResponseMessage(int status, int response, int intent, int featureFlags) {
        final MessageWriter writer = new MessageWriter(12);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_RESPONSE);
        writer.writeShort(VivomoveConstants.MESSAGE_NOTIFICATION_SERVICE_SUBSCRIPTION);
        writer.writeByte(status);
        writer.writeByte(response);
        writer.writeByte(intent);
        writer.writeByte(featureFlags);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

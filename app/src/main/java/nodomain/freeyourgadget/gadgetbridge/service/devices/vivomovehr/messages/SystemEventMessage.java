package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.GarminSystemEventType;

public class SystemEventMessage {
    public final byte[] packet;

    public SystemEventMessage(GarminSystemEventType eventType, Object value) {
        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_SYSTEM_EVENT);
        writer.writeByte(eventType.ordinal());
        if (value instanceof String) {
            writer.writeString((String) value);
        } else if (value instanceof Integer) {
            writer.writeByte((Integer) value);
        } else {
            throw new IllegalArgumentException("Unsupported event value type " + value);
        }
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

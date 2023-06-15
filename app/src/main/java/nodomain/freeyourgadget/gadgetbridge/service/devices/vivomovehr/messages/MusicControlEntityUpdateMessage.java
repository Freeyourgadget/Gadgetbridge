package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ams.AmsEntityAttribute;

public class MusicControlEntityUpdateMessage {
    public final byte[] packet;

    public MusicControlEntityUpdateMessage(AmsEntityAttribute... attributes) {
        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_MUSIC_CONTROL_ENTITY_UPDATE);
        for (AmsEntityAttribute attribute : attributes) {
            attribute.writeToMessage(writer);
        }
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

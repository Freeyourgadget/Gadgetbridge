package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.GarminMusicControlCommand;

public class MusicControlCapabilitiesResponseMessage {
    public final byte[] packet;

    public MusicControlCapabilitiesResponseMessage(int status, GarminMusicControlCommand[] commands) {
        if (commands.length > 255) throw new IllegalArgumentException("Too many supported commands");
        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_RESPONSE);
        writer.writeShort(VivomoveConstants.MESSAGE_MUSIC_CONTROL_CAPABILITIES);
        writer.writeByte(status);
        writer.writeByte(commands.length);
        for (GarminMusicControlCommand command : commands) {
            writer.writeByte(command.ordinal());
        }
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

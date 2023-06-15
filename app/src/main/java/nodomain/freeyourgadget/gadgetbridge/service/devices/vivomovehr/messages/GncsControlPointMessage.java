package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsControlCommand;

public class GncsControlPointMessage {
    public final AncsControlCommand command;

    public GncsControlPointMessage(AncsControlCommand command) {
        this.command = command;
    }

    public static GncsControlPointMessage parsePacket(byte[] packet) {
        final AncsControlCommand command = AncsControlCommand.parseCommand(packet, 4, packet.length - 6);
        if (command == null) return null;
        return new GncsControlPointMessage(command);
    }
}

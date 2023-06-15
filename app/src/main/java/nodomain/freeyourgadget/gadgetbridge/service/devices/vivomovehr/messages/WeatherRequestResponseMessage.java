package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class WeatherRequestResponseMessage {
    public final byte[] packet;

    public WeatherRequestResponseMessage(int status, int requestStatus, int totalDataMessages, int timeout) {
        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_RESPONSE);
        writer.writeShort(VivomoveConstants.MESSAGE_WEATHER_REQUEST);
        writer.writeByte(status);
        writer.writeByte(requestStatus);
        writer.writeByte(totalDataMessages);
        writer.writeShort(timeout);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class DeviceInformationResponseMessage {
    public final byte[] packet;

    public DeviceInformationResponseMessage(int status, int protocolVersion, int productNumber, int unitNumber, int softwareVersion, int maxPacketSize, String bluetoothFriendlyName, String deviceName, String deviceModel, int protocolFlags) {
        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_RESPONSE);
        writer.writeShort(VivomoveConstants.MESSAGE_DEVICE_INFORMATION);
        writer.writeByte(status);
        writer.writeShort(protocolVersion);
        writer.writeShort(productNumber);
        writer.writeInt(unitNumber);
        writer.writeShort(softwareVersion);
        writer.writeShort(maxPacketSize);
        writer.writeString(bluetoothFriendlyName);
        writer.writeString(deviceName);
        writer.writeString(deviceModel);
        writer.writeByte(protocolFlags);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}

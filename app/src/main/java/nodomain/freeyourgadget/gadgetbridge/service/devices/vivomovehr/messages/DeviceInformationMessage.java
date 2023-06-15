package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import java.util.Locale;

public class DeviceInformationMessage {
    public final int protocolVersion;
    public final int productNumber;
    public final String unitNumber;
    public final int softwareVersion;
    public final int maxPacketSize;
    public final String bluetoothFriendlyName;
    public final String deviceName;
    public final String deviceModel;
    // dual-pairing flags & MAC addresses...

    public DeviceInformationMessage(int protocolVersion, int productNumber, String unitNumber, int softwareVersion, int maxPacketSize, String bluetoothFriendlyName, String deviceName, String deviceModel) {
        this.protocolVersion = protocolVersion;
        this.productNumber = productNumber;
        this.unitNumber = unitNumber;
        this.softwareVersion = softwareVersion;
        this.maxPacketSize = maxPacketSize;
        this.bluetoothFriendlyName = bluetoothFriendlyName;
        this.deviceName = deviceName;
        this.deviceModel = deviceModel;
    }

    public static DeviceInformationMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int protocolVersion = reader.readShort();
        final int productNumber = reader.readShort();
        final String unitNumber = Long.toString(reader.readInt() & 0xFFFFFFFFL);
        final int softwareVersion = reader.readShort();
        final int maxPacketSize = reader.readShort();
        final String bluetoothFriendlyName = reader.readString();
        final String deviceName = reader.readString();
        final String deviceModel = reader.readString();

        return new DeviceInformationMessage(protocolVersion, productNumber, unitNumber, softwareVersion, maxPacketSize, bluetoothFriendlyName, deviceName, deviceModel);
    }

    public String getSoftwareVersionStr() {
        int softwareVersionMajor = softwareVersion / 100;
        int softwareVersionMinor = softwareVersion % 100;
        return String.format(Locale.ROOT, "%d.%02d", softwareVersionMajor, softwareVersionMinor);
    }
}

package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;

public class DeviceInformationMessage extends GFDIMessage {

    final int ourUnitNumber = -1;
    final int ourSoftwareVersion = 7791;
    final int ourMaxPacketSize = -1;
    private final int messageType;
    private final int incomingProtocolVersion;
    private final int ourProtocolVersion = 150;
    private final int incomingProductNumber;
    private final int ourProductNumber = -1;
    private final String incomingUnitNumber;
    private final int incomingSoftwareVersion;
    private final int incomingMaxPacketSize;
    private final String bluetoothFriendlyName;
    private final String deviceName;
    private final String deviceModel;
    // dual-pairing flags & MAC addresses...

    public DeviceInformationMessage(int messageType, int protocolVersion, int productNumber, String unitNumber, int softwareVersion, int maxPacketSize, String bluetoothFriendlyName, String deviceName, String deviceModel) {
        this.messageType = messageType;
        this.incomingProtocolVersion = protocolVersion;
        this.incomingProductNumber = productNumber;
        this.incomingUnitNumber = unitNumber;
        this.incomingSoftwareVersion = softwareVersion;
        this.incomingMaxPacketSize = maxPacketSize;
        this.bluetoothFriendlyName = bluetoothFriendlyName;
        this.deviceName = deviceName;
        this.deviceModel = deviceModel;
    }

    public static DeviceInformationMessage parseIncoming(MessageReader reader, int messageType) {
        final int protocolVersion = reader.readShort();
        final int productNumber = reader.readShort();
        final String unitNumber = Long.toString(reader.readInt() & 0xFFFFFFFFL);
        final int softwareVersion = reader.readShort();
        final int maxPacketSize = reader.readShort();
        final String bluetoothFriendlyName = reader.readString();
        final String deviceName = reader.readString();
        final String deviceModel = reader.readString();

        reader.warnIfLeftover();
        return new DeviceInformationMessage(messageType, protocolVersion, productNumber, unitNumber, softwareVersion, maxPacketSize, bluetoothFriendlyName, deviceName, deviceModel);
    }

    @Override
    protected boolean generateOutgoing() {

        final int protocolFlags = this.incomingProtocolVersion / 100 == 1 ? 1 : 0;

        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // placeholder for packet size
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(messageType);
        writer.writeByte(Status.ACK.ordinal());
        writer.writeShort(ourProtocolVersion);
        writer.writeShort(ourProductNumber);
        writer.writeInt(ourUnitNumber);
        writer.writeShort(ourSoftwareVersion);
        writer.writeShort(ourMaxPacketSize);
        writer.writeString(BluetoothAdapter.getDefaultAdapter().getName());
        writer.writeString(Build.MANUFACTURER);
        writer.writeString(Build.DEVICE);
        writer.writeByte(protocolFlags);
        return true;
    }

    public GBDeviceEventVersionInfo getGBDeviceEvent() {

        LOG.info("Received device information: protocol {}, product {}, unit {}, SW {}, max packet {}, BT name {}, device name {}, device model {}", incomingProtocolVersion, incomingProductNumber, incomingUnitNumber, getSoftwareVersionStr(), incomingMaxPacketSize, bluetoothFriendlyName, deviceName, deviceModel);

        GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
        versionCmd.fwVersion = getSoftwareVersionStr();
        versionCmd.hwVersion = deviceModel;
        return versionCmd;
    }

    private String getSoftwareVersionStr() {
        int softwareVersionMajor = incomingSoftwareVersion / 100;
        int softwareVersionMinor = incomingSoftwareVersion % 100;
        return String.format(Locale.ROOT, "%d.%02d", softwareVersionMajor, softwareVersionMinor);
    }
}

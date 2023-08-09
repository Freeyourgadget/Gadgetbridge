/*  Copyright (C) 2020-2023 Petr Kadlec

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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

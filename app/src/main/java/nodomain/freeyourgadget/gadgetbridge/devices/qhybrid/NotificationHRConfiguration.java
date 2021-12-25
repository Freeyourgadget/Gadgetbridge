/*  Copyright (C) 2019-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

public class NotificationHRConfiguration implements Serializable {
    private String packageName;
    private String iconName;
    private byte[] packageCrc;

    public NotificationHRConfiguration(String packageName, String iconName) {
        this.packageName = packageName;
        this.iconName = iconName;

        CRC32 crc = new CRC32();
        crc.update(packageName.getBytes());

        this.packageCrc = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) crc.getValue())
                .array();
    }

    public NotificationHRConfiguration(String packageName, byte[] packageCrc, String iconName) {
        this.packageCrc = packageCrc;
        this.packageName = packageName;
        this.iconName = iconName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getIconName() {
        return iconName;
    }

    public byte[] getPackageCrc() {
        return packageCrc;
    }
}

/*  Copyright (C) 2017-2021 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbipu;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband4.MiBand4FirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class AmazfitBipUFirmwareInfo extends HuamiFirmwareInfo {

    private static Map<Integer, String> crcToVersion = new HashMap<>();

    static {
        // fw
        crcToVersion.put(50652, "1.0.2.95");

        // resources
        crcToVersion.put(52508, "1.0.2.95");

        // font
        crcToVersion.put(60101, "8");

    }

    public AmazfitBipUFirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {

        if (ArrayUtils.equals(bytes, MiBand4FirmwareInfo.FW_HEADER, MiBand4FirmwareInfo.FW_HEADER_OFFSET)) {
            if (searchString32BitAligned(bytes, "\0\0Amazfit Bip U")) {
                return HuamiFirmwareType.FIRMWARE;
            }
            return HuamiFirmwareType.INVALID;
        }

        if (ArrayUtils.startsWith(bytes,NEWRES_HEADER)) {
            return HuamiFirmwareType.RES;
        }

        if (ArrayUtils.startsWith(bytes, UIHH_HEADER) && (bytes[4] == 1 || bytes[4] == 2)) {
            return HuamiFirmwareType.WATCHFACE;
        }

        if (ArrayUtils.startsWith(bytes, NEWFT_HEADER)) {
            if (bytes[10] == 0x01 || bytes[10] == 0x06 || bytes[10] == 0x03) {
                return HuamiFirmwareType.FONT;
            }
        }

        return HuamiFirmwareType.INVALID;
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITBIPU;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }
}

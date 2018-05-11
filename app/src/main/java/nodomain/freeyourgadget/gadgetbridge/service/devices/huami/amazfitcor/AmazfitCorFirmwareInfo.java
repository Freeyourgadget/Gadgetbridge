/*  Copyright (C) 2017-2018 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitcor;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

public class AmazfitCorFirmwareInfo extends HuamiFirmwareInfo {
    // this is the same as Bip
    private static final byte[] FW_HEADER = new byte[]{
            0x00, (byte) 0x98, 0x00, 0x20, (byte) 0xA5, 0x04, 0x00, 0x20, (byte) 0xAD, 0x04, 0x00, 0x20, (byte) 0xC5, 0x04, 0x00, 0x20
    };

    private static final int COMPRESSED_RES_HEADER_OFFSET = 0x9;

    private static Map<Integer, String> crcToVersion = new HashMap<>();

    static {
        // firmware
        crcToVersion.put(39948, "1.0.5.60");
        crcToVersion.put(62147, "1.0.5.78");
        crcToVersion.put(54213, "1.0.6.76");

        // resources
        crcToVersion.put(46341, "RES 1.0.5.60");
        crcToVersion.put(21770, "RES 1.0.5.78");
        crcToVersion.put(64977, "RES 1.0.6.76");

    }

    public AmazfitCorFirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.startsWith(bytes, RES_HEADER)) {
            if (bytes.length < 700000) { // dont know how to distinguish from Bip .res
                return HuamiFirmwareType.INVALID;
            }
            return HuamiFirmwareType.RES;
        }
        if (ArrayUtils.equals(bytes, RES_HEADER, COMPRESSED_RES_HEADER_OFFSET) || ArrayUtils.equals(bytes, NEWRES_HEADER, COMPRESSED_RES_HEADER_OFFSET)) {
            return HuamiFirmwareType.RES_COMPRESSED;
        }
        if (ArrayUtils.startsWith(bytes, FW_HEADER)) {
            String foundVersion = searchFirmwareVersion(bytes);
            if (foundVersion != null) {
                Version version = new Version(foundVersion);
                if ((version.compareTo(new Version("1.0.5.00")) >= 0) && (version.compareTo(new Version("2.0.0.00")) < 0)) {
                    return HuamiFirmwareType.FIRMWARE;
                }
            }
            return HuamiFirmwareType.INVALID;
        }
        if (ArrayUtils.startsWith(bytes, WATCHFACE_HEADER)) {
            return HuamiFirmwareType.WATCHFACE;
        }

        return HuamiFirmwareType.INVALID;
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITCOR;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }
}

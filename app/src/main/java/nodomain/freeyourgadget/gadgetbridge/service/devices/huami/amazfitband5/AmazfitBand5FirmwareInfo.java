/*  Copyright (C) 2017-2021 Andreas Shimokawa, Cristian Alfano, Daniele
    Gobbetti, odavo32nof

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitband5;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class AmazfitBand5FirmwareInfo extends HuamiFirmwareInfo {

    public static final byte[] FW_HEADER = new byte[]{
            0x31, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x9c, (byte) 0xe3, 0x7d, 0x5c, 0x00, 0x04
    };

    public static final int FW_HEADER_OFFSET = 16;

    private static Map<Integer, String> crcToVersion = new HashMap<>();

    static {
        // no known fw so far
    }

    public AmazfitBand5FirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.equals(bytes, RES_HEADER, COMPRESSED_RES_HEADER_OFFSET) || ArrayUtils.equals(bytes, NEWRES_HEADER, COMPRESSED_RES_HEADER_OFFSET_NEW) || ArrayUtils.equals(bytes, NEWRES_HEADER, COMPRESSED_RES_HEADER_OFFSET)) {
            return HuamiFirmwareType.RES_COMPRESSED;
        }
        if (ArrayUtils.equals(bytes, FW_HEADER, FW_HEADER_OFFSET)) {
            if (searchString32BitAligned(bytes, "Amazfit Band 5")) {
                return HuamiFirmwareType.FIRMWARE;
            }
            return HuamiFirmwareType.INVALID;
        }
        if (ArrayUtils.startsWith(bytes, UIHH_HEADER) && (bytes[4] == 1 || bytes[4] == 2)) {
            return HuamiFirmwareType.WATCHFACE;
        }
        if (ArrayUtils.startsWith(bytes, NEWFT_HEADER)) {
            if (bytes[10] == 0x03 || bytes[10] == 0x06) {
                return HuamiFirmwareType.FONT;
            }
        }
        // somebody might have unpacked the compressed res
        if (ArrayUtils.startsWith(bytes, RES_HEADER)) {
            return HuamiFirmwareType.RES;
        }
        return HuamiFirmwareType.INVALID;
    }


    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITBAND5;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }
}

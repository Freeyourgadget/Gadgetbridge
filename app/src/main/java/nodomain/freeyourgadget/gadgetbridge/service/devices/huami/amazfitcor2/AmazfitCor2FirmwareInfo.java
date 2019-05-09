/*  Copyright (C) 2017-2019 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitcor2;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class AmazfitCor2FirmwareInfo extends HuamiFirmwareInfo {
    // this is the same as Bip
    private static final byte[] FW_HEADER = new byte[]{
            0x00, (byte) 0x98, 0x00, 0x20, (byte) 0xA5, 0x04, 0x00, 0x20, (byte) 0xAD, 0x04, 0x00, 0x20, (byte) 0xC5, 0x04, 0x00, 0x20
    };

    private static final int COMPRESSED_RES_HEADER_OFFSET = 0x9;
    private static final int COMPRESSED_RES_HEADER_OFFSET_NEW = 0xd;

    private static Map<Integer, String> crcToVersion = new HashMap<>();

    static {
        // font
        crcToVersion.put(61054, "8");
        crcToVersion.put(62291, "9 (Latin)");
    }

    public AmazfitCor2FirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.equals(bytes, RES_HEADER, COMPRESSED_RES_HEADER_OFFSET) || ArrayUtils.equals(bytes, NEWRES_HEADER, COMPRESSED_RES_HEADER_OFFSET_NEW) || ArrayUtils.equals(bytes, NEWRES_HEADER, COMPRESSED_RES_HEADER_OFFSET)) {
            return HuamiFirmwareType.RES_COMPRESSED;
        }
        if (ArrayUtils.startsWith(bytes, FW_HEADER)) {
            // FIXME: It would certainly better if we could check for "Cor 2" when the device name is "Cor 2" and for "Band 2" when it is "Band 2"
            if (searchString32BitAligned(bytes, "Amazfit Cor 2")) {
                return HuamiFirmwareType.FIRMWARE;
            }
            if (searchString32BitAligned(bytes, "Amazfit Band 2")) {
                return HuamiFirmwareType.FIRMWARE;
            }
            return HuamiFirmwareType.INVALID;
        }
        if (ArrayUtils.startsWith(bytes, WATCHFACE_HEADER)) {
            return HuamiFirmwareType.WATCHFACE;
        }
        if (ArrayUtils.startsWith(bytes, NEWFT_HEADER)) {
            if (bytes[10] == 0x01) {
                return HuamiFirmwareType.FONT;
            } else if (bytes[10] == 0x02) {
                return HuamiFirmwareType.FONT_LATIN;
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
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITCOR2;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }
}

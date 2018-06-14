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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband3;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class MiBand3FirmwareInfo extends HuamiFirmwareInfo {
    // this is the same as Mi Band 2
    private static final byte[] FW_HEADER = new byte[]{
            (byte) 0xa3, (byte) 0x68, (byte) 0x04, (byte) 0x3b, (byte) 0x02, (byte) 0xdb,
            (byte) 0xc8, (byte) 0x58, (byte) 0xd0, (byte) 0x50, (byte) 0xfa, (byte) 0xe7,
            (byte) 0x0c, (byte) 0x34, (byte) 0xf3, (byte) 0xe7,
    };

    private static final int FW_HEADER_OFFSET = 0x150;

    private static final byte FW_MAGIC = (byte) 0xf9;
    private static final int FW_MAGIC_OFFSET = 0x17d;

    private static Map<Integer, String> crcToVersion = new HashMap<>();

    static {
        // firmware

        // resources
    }

    public MiBand3FirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.startsWith(bytes, RES_HEADER)) {
            if (bytes.length > 100000) { // don't know how to distinguish from Bip/Cor .res
                return HuamiFirmwareType.INVALID;
            }
            return HuamiFirmwareType.RES;
        }
        if (ArrayUtils.equals(bytes, FW_HEADER, FW_HEADER_OFFSET)
                && (bytes[FW_MAGIC_OFFSET] == FW_MAGIC)) {
            // TODO: this is certainly not a correct validation, but it works for now
            return HuamiFirmwareType.FIRMWARE;
        }
        return HuamiFirmwareType.INVALID;
    }


    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.MIBAND3;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }

    @Override
    protected String searchFirmwareVersion(byte[] fwbytes) {
        // does not work for Mi Band 3
        return null;
    }
}

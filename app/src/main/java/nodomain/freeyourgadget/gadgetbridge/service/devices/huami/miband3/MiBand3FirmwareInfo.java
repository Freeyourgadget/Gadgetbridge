/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer, Davis
    Mosenkovs

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
        crcToVersion.put(55852, "1.2.0.8");
        crcToVersion.put(14899, "1.3.0.4");
        crcToVersion.put(20651, "1.3.0.8");
        crcToVersion.put(60781, "1.4.0.12");
        crcToVersion.put(30045, "1.5.0.2");
        crcToVersion.put(38254, "1.5.0.7");
        crcToVersion.put(46985, "1.5.0.11");
        crcToVersion.put(31330, "1.6.0.16");
        crcToVersion.put(10930, "1.8.0.0");
        crcToVersion.put(59800, "2.0.0.4");
        crcToVersion.put(10023, "2.2.0.12");

        // resources
        crcToVersion.put(54724, "1.2.0.8");
        crcToVersion.put(52589, "1.3.0.4");
        crcToVersion.put(34642, "1.3.0.8");
        crcToVersion.put(25278, "1.4.0.12-1.6.0.16");
        crcToVersion.put(23249, "1.8.0.0");
        crcToVersion.put(1815, "2.0.0.4");
        crcToVersion.put(7225, "2.2.0.12");

        // font
        crcToVersion.put(19775, "1");
    }

    public MiBand3FirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.startsWith(bytes, FT_HEADER)) {
            if (bytes[FONT_TYPE_OFFSET] == 0x03 || bytes[FONT_TYPE_OFFSET] == 0x04) {
                return HuamiFirmwareType.FONT;
            }
            return HuamiFirmwareType.INVALID;
        }
        if (ArrayUtils.startsWith(bytes, RES_HEADER)) {
            if (bytes.length > 150000) { // don't know how to distinguish from Bip/Cor .res
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

/*  Copyright (C) 2017-2020 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbips;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband4.MiBand4FirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class AmazfitBipSFirmwareInfo extends HuamiFirmwareInfo {

    private static Map<Integer, String> crcToVersion = new HashMap<>();

    public AmazfitBipSFirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        //FW "header" looks like Mi Band 4
        if (ArrayUtils.equals(bytes, MiBand4FirmwareInfo.FW_HEADER, MiBand4FirmwareInfo.FW_HEADER_OFFSET)) {
            if (searchString32BitAligned(bytes, "Amazfit Bip S")) {
                return HuamiFirmwareType.FIRMWARE;
            }
            return HuamiFirmwareType.INVALID;
        }
        if (ArrayUtils.equals(bytes, NEWRES_HEADER, COMPRESSED_RES_HEADER_OFFSET)) {
            return HuamiFirmwareType.RES_COMPRESSED;
        }

        if (ArrayUtils.startsWith(bytes, WATCHFACE_HEADER)) {
            return HuamiFirmwareType.WATCHFACE;
        }
        if (ArrayUtils.startsWith(bytes, GPS_CEP_HEADER)) {
            return HuamiFirmwareType.GPS_CEP;
        }
        if (ArrayUtils.startsWith(bytes, GPS_CEP_HEADER)) {
            return HuamiFirmwareType.GPS_CEP;
        }
        if (ArrayUtils.startsWith(bytes, NEWFT_HEADER)) {
            if (bytes[10] == 0x01 || bytes[10] == 0x06) {
                return HuamiFirmwareType.FONT;
            } else if (bytes[10] == 0x02 || bytes[10] == 0x0A) {
                return HuamiFirmwareType.FONT_LATIN;
            }
        }
        return HuamiFirmwareType.INVALID;
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITBIPS;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }
}

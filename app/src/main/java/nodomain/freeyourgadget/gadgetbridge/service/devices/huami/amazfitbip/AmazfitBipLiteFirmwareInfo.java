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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class AmazfitBipLiteFirmwareInfo extends HuamiFirmwareInfo {

    // this is the same as Bip and Cor
    private static final byte[] FW_HEADER = new byte[]{
            0x00, (byte) 0x98, 0x00, 0x20, (byte) 0xA5, 0x04, 0x00, 0x20, (byte) 0xAD, 0x04, 0x00, 0x20, (byte) 0xC5, 0x04, 0x00, 0x20
    };


    private static Map<Integer, String> crcToVersion = new HashMap<>();

    static {
        // firmware
        crcToVersion.put(11059, "1.1.6.02");

        // Latin Firmware


        // resources
        crcToVersion.put(57510, "1.1.6.02");

        // font
        crcToVersion.put(61054, "8");
        crcToVersion.put(59577, "9 (Latin)");
    }

    public AmazfitBipLiteFirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.startsWith(bytes, NEWRES_HEADER)) {
            if ((bytes.length <= 100000) || (bytes.length > 700000)) { // don't know how to distinguish from Cor/Mi Band 3 .res
                return HuamiFirmwareType.INVALID;
            }
            return HuamiFirmwareType.RES;
        }
        if (ArrayUtils.startsWith(bytes, FW_HEADER)) {
            if (searchString32BitAligned(bytes, "Amazfit Bip Lite")) {
                return HuamiFirmwareType.FIRMWARE;
            }
            GBDevice device = GBApplication.app().getDeviceManager().getSelectedDevice();
            if (device != null) {
                Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
                if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_RELAX_FIRMWARE_CHECKS, false)) {
                    if (searchString32BitAligned(bytes, "Amazfit Bip")) {
                        return HuamiFirmwareType.FIRMWARE;
                    }
                }
            }
            return HuamiFirmwareType.INVALID;
        }
        if (ArrayUtils.startsWith(bytes, WATCHFACE_HEADER)) {
            return HuamiFirmwareType.WATCHFACE;
        }
        if (ArrayUtils.startsWith(bytes, NEWFT_HEADER)) {
            if (bytes[10] == 0x01) {
                return HuamiFirmwareType.FONT;
            } else if (bytes[10] == 0x02 || bytes[10] == 0x0A) {
                return HuamiFirmwareType.FONT_LATIN;
            }
        }

        return HuamiFirmwareType.INVALID;
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITBIP_LITE;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }
}

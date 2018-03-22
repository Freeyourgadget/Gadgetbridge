/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

/**
 * FW1 is Mi Band firmware
 * FW2 is heartrate firmware
 */
public class Mi1SFirmwareInfo extends CompositeMiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(Mi1SFirmwareInfo.class);

    private static final byte[] DOUBLE_FW_HEADER = new byte[]{
            (byte) 0x78,
            (byte) 0x75,
            (byte) 0x63,
            (byte) 0x6b
    };
    private static final int DOUBLE_FW_HEADER_OFFSET = 0;

    private Mi1SFirmwareInfo(byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes, new Mi1SFirmwareInfoFW1(wholeFirmwareBytes), new Mi1SFirmwareInfoFW2(wholeFirmwareBytes));
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return MiBandConst.MI_1S.equals(device.getModel());
    }

    @Override
    public boolean isSingleMiBandFirmware() {
        return false;
    }

    @Override
    protected boolean isHeaderValid() {
        // TODO: not sure if this is a correct check!
        return ArrayUtils.equals(wholeFirmwareBytes, DOUBLE_FW_HEADER, DOUBLE_FW_HEADER_OFFSET);
    }

    @Nullable
    public static Mi1SFirmwareInfo getInstance(byte[] wholeFirmwareBytes) {
        Mi1SFirmwareInfo info = new Mi1SFirmwareInfo(wholeFirmwareBytes);
        if (info.isGenerallySupportedFirmware()) {
            return info;
        }
        LOG.info("firmware not supported");
        return null;
    }
}

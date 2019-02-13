/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;

/**
 * FW1 is Mi Band firmware
 * FW2 is heartrate firmware
 */
public class Mi1SFirmwareInfoFW1 extends AbstractMi1SFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(Mi1SFirmwareInfoFW1.class);
    private static final int MI1S_FW_BASE_OFFSET = 1092;

    Mi1SFirmwareInfoFW1(@NonNull byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
    }

    @Override
    protected boolean isHeaderValid() {
        return true;
    }

    @Override
    public int getFirmwareOffset() {
        return (wholeFirmwareBytes[12] & 255) << 24
                | (wholeFirmwareBytes[13] & 255) << 16
                | (wholeFirmwareBytes[14] & 255) << 8
                | (wholeFirmwareBytes[15] & 255);
    }

    @Override
    public int getFirmwareLength() {
        return (wholeFirmwareBytes[16] & 255) << 24
                | (wholeFirmwareBytes[17] & 255) << 16
                | (wholeFirmwareBytes[18] & 255) << 8
                | (wholeFirmwareBytes[19] & 255);
    }

    @Override
    public int getFirmwareVersion() {
        return (wholeFirmwareBytes[8] & 255) << 24
                | (wholeFirmwareBytes[9] & 255) << 16
                | (wholeFirmwareBytes[10] & 255) << 8
                | wholeFirmwareBytes[11] & 255;
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        try {
            int majorVersion = getFirmwareVersionMajor();
            if (majorVersion == 4) {
                return true;
            } else {
                LOG.warn("Only major version 4 is supported for 1S fw1: " + majorVersion);
            }
        } catch (IllegalArgumentException ex) {
            LOG.warn("not supported 1S firmware 1: " + ex.getLocalizedMessage(), ex);
        }
        return false;
    }
}

/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer

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

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FW1 is Mi Band firmware
 * FW2 is heartrate firmware
 */
public class Mi1SFirmwareInfoFW2 extends AbstractMi1SFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(Mi1SFirmwareInfoFW2.class);

    Mi1SFirmwareInfoFW2(@NonNull byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
    }

    @Override
    protected boolean isHeaderValid() {
        return true;
    }

    @Override
    public int getFirmwareOffset() {
        return (wholeFirmwareBytes[26] & 255) << 24
                | (wholeFirmwareBytes[27] & 255) << 16
                | (wholeFirmwareBytes[28] & 255) << 8
                | (wholeFirmwareBytes[29] & 255);
    }

    @Override
    public int getFirmwareLength() {
        return (wholeFirmwareBytes[30] & 255) << 24
                | (wholeFirmwareBytes[31] & 255) << 16
                | (wholeFirmwareBytes[32] & 255) << 8
                | (wholeFirmwareBytes[33] & 255);
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        try {
            int majorVersion = getFirmwareVersionMajor();
            if (majorVersion == 1) {
                return true;
            } else {
                LOG.warn("Only major version 1 is supported for 1S fw2: " + majorVersion);
            }
        } catch (IllegalArgumentException ex) {
            LOG.warn("not supported 1S firmware 2: " + ex.getLocalizedMessage(), ex);
        }
        return false;
    }

    @Override
    public int getFirmwareVersion() {
        return (wholeFirmwareBytes[22] & 255) << 24
                | (wholeFirmwareBytes[23] & 255) << 16
                | (wholeFirmwareBytes[24] & 255) << 8
                | wholeFirmwareBytes[25] & 255;
    }
}

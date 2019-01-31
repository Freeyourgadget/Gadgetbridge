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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.Nullable;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * This is a class just for testing the dual fw firmware update procedure.
 * It uses two instances of the known-to-be-working Mi1A firmware update instances
 * and combines them in a CompositeMiFirmwareInfo.
 * <p/>
 * Most methods simply delegate to one of the child instances (FW1).
 * <p/>
 * FW1 is the default Mi 1A Band firmware
 * FW2 is the same default Mi 1A Band firmware
 */
public class TestMi1AFirmwareInfo extends CompositeMiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(TestMi1AFirmwareInfo.class);

    private TestMi1AFirmwareInfo(byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes, new Mi1AFirmwareInfo(wholeFirmwareBytes), new Mi1AFirmwareInfo(wholeFirmwareBytes));
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
//        super.checkValid();
// unfortunately we cannot use all of the checks in the superclass, so we roll our own

        if (getFirst().getFirmwareOffset() != getSecond().getFirmwareOffset()) {
            throw new IllegalArgumentException("Test firmware offsets should be the same: " + getLengthsOffsetsString());
        }
        if (getFirst().getFirmwareOffset() < 0 || getSecond().getFirmwareOffset() < 0
                || getFirst().getFirmwareLength() <= 0 || getSecond().getFirmwareLength() <= 0) {
            throw new IllegalArgumentException("Illegal test firmware offsets/lengths: " + getLengthsOffsetsString());
        }

        if (getFirst().getFirmwareLength() != getSecond().getFirmwareLength()) {
            throw new IllegalArgumentException("Illegal test firmware lengths: " + getLengthsOffsetsString());
        }
        int firstEndIndex = getFirst().getFirmwareOffset() + getFirst().getFirmwareLength();
        int secondEndIndex = getSecond().getFirmwareOffset();
        if (wholeFirmwareBytes.length < firstEndIndex || wholeFirmwareBytes.length < secondEndIndex) {
            throw new IllegalArgumentException("Invalid test firmware size, or invalid test offsets/lengths: " + getLengthsOffsetsString());
        }

        getFirst().checkValid();
        getSecond().checkValid();
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return getFirst().isGenerallyCompatibleWith(device);
    }

    @Override
    public boolean isSingleMiBandFirmware() {
        return false;
    }

    protected boolean isHeaderValid() {
        return getFirst().isHeaderValid();
    }

    @Nullable
    public static TestMi1AFirmwareInfo getInstance(byte[] wholeFirmwareBytes) {
        TestMi1AFirmwareInfo info = new TestMi1AFirmwareInfo(wholeFirmwareBytes);
        if (info.isGenerallySupportedFirmware()) {
            return info;
        }
        LOG.info("firmware not supported");
        return null;
    }
}

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

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

/**
 * Some helper methods for Mi1 and Mi1A firmware.
 */
public abstract class AbstractMi1FirmwareInfo extends AbstractMiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMi1FirmwareInfo.class);

    private static final byte[] SINGLE_FW_HEADER = new byte[]{
            0,
            (byte) 0x98,
            0,
            (byte) 0x20,
            (byte) 0x89,
            4,
            0,
            (byte) 0x20
    };
    private static final int SINGLE_FW_HEADER_OFFSET = 0;

    private static final int MI1_FW_BASE_OFFSET = 1056;

    protected AbstractMi1FirmwareInfo(@NonNull byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
    }

    @Override
    public boolean isSingleMiBandFirmware() {
        return true;
    }

    @Override
    public int getFirmwareOffset() {
        return 0;
    }

    @Override
    public int getFirmwareLength() {
        return wholeFirmwareBytes.length;
    }

    @Override
    public int getFirmwareVersion() {
        return (wholeFirmwareBytes[getOffsetFirmwareVersionMajor()] << 24)
                | (wholeFirmwareBytes[getOffsetFirmwareVersionMinor()] << 16)
                | (wholeFirmwareBytes[getOffsetFirmwareVersionRevision()] << 8)
                | wholeFirmwareBytes[getOffsetFirmwareVersionBuild()];
    }

    private int getOffsetFirmwareVersionMajor() {
        return MI1_FW_BASE_OFFSET + 3;
    }

    private int getOffsetFirmwareVersionMinor() {
        return MI1_FW_BASE_OFFSET + 2;
    }

    private int getOffsetFirmwareVersionRevision() {
        return MI1_FW_BASE_OFFSET + 1;
    }

    private int getOffsetFirmwareVersionBuild() {
        return MI1_FW_BASE_OFFSET;
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        try {
            if (!isHeaderValid()) {
                LOG.info("unrecognized header");
                return false;
            }
            int majorVersion = getFirmwareVersionMajor();
            if (majorVersion == getSupportedMajorVersion()) {
                return true;
            } else {
                LOG.info("Only major version " + getSupportedMajorVersion() + " is supported: " + majorVersion);
            }
        } catch (IllegalArgumentException ex) {
            LOG.warn("invalid firmware or bug: " + ex.getLocalizedMessage(), ex);
        } catch (IndexOutOfBoundsException ex) {
            LOG.warn("not supported firmware: " + ex.getLocalizedMessage(), ex);
        }
        return false;
    }

    @Override
    protected boolean isHeaderValid() {
        // TODO: not sure if this is a correct check!
        return ArrayUtils.equals(wholeFirmwareBytes, SINGLE_FW_HEADER, SINGLE_FW_HEADER_OFFSET);
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        super.checkValid();

        if (wholeFirmwareBytes.length < SINGLE_FW_HEADER.length) {
            throw new IllegalArgumentException("firmware too small: " + wholeFirmwareBytes.length);
        }
    }

    protected abstract int getSupportedMajorVersion();
}

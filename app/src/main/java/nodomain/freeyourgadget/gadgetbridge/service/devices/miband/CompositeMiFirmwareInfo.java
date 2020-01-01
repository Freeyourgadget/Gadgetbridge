/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer

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

/**
 * Abstract Mi firmware info class with two child info instances.
 */
public abstract class CompositeMiFirmwareInfo<T extends AbstractMiFirmwareInfo> extends AbstractMiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeMiFirmwareInfo.class);

    private final T fw1Info;
    private final T fw2Info;

    protected CompositeMiFirmwareInfo(byte[] wholeFirmwareBytes, T info1, T info2) {
        super(wholeFirmwareBytes);
        fw1Info = info1;
        fw2Info = info2;
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        super.checkValid();

        if (getFirst().getFirmwareOffset() == getSecond().getFirmwareOffset()) {
            throw new IllegalArgumentException("Illegal firmware offsets: " + getLengthsOffsetsString());
        }
        if (getFirst().getFirmwareOffset() < 0 || getSecond().getFirmwareOffset() < 0
                || getFirst().getFirmwareLength() <= 0 || getSecond().getFirmwareLength() <= 0) {
            throw new IllegalArgumentException("Illegal firmware offsets/lengths: " + getLengthsOffsetsString());
        }

        int firstEndIndex = getFirst().getFirmwareOffset() + getFirst().getFirmwareLength();
        if (getSecond().getFirmwareOffset() < firstEndIndex) {
            throw new IllegalArgumentException("Invalid firmware, second fw starts before first fw ends: " + firstEndIndex + "," + getSecond().getFirmwareOffset());
        }
        int secondEndIndex = getSecond().getFirmwareOffset();
        if (wholeFirmwareBytes.length < firstEndIndex || wholeFirmwareBytes.length < secondEndIndex) {
            throw new IllegalArgumentException("Invalid firmware size, or invalid offsets/lengths: " + getLengthsOffsetsString());
        }

        getFirst().checkValid();
        getSecond().checkValid();
    }

    protected String getLengthsOffsetsString() {
        return getFirst().getFirmwareOffset() + "," + getFirst().getFirmwareLength()
                + "; "
                + getSecond().getFirmwareOffset() + "," + getSecond().getFirmwareLength();
    }

    @Override
    public T getFirst() {
        return fw1Info;
    }

    @Override
    public T getSecond() {
        return fw2Info;
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        try {
            if (!isHeaderValid()) {
                LOG.info("unrecognized header");
                return false;
            }
            return fw1Info.isGenerallySupportedFirmware()
                    && fw2Info.isGenerallySupportedFirmware()
                    && fw1Info.getFirmwareBytes().length > 0
                    && fw2Info.getFirmwareBytes().length > 0;
        } catch (IndexOutOfBoundsException ex) {
            LOG.warn("invalid firmware or bug: " + ex.getLocalizedMessage(), ex);
            return false;
        } catch (IllegalArgumentException ex) {
            LOG.warn("not supported 1S firmware: " + ex.getLocalizedMessage(), ex);
            return false;
        }
    }

    @Override
    public int getFirmwareOffset() {
        throw new UnsupportedOperationException("call this method on getFirmwareXInfo()");
    }

    @Override
    public int getFirmwareLength() {
        throw new UnsupportedOperationException("call this method on getFirmwareXInfo()");
    }

    @Override
    public int getFirmwareVersion() {
        throw new UnsupportedOperationException("call this method on getFirmwareXInfo()");
    }
}

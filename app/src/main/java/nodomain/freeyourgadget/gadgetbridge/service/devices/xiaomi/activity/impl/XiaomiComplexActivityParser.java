/*  Copyright (C) 2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class XiaomiComplexActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiComplexActivityParser.class);

    private final byte[] header;
    private final ByteBuffer buf;

    private int currentGroup = -1;
    private int currentGroupBits;
    private int currentVal;

    public XiaomiComplexActivityParser(final byte[] header, final ByteBuffer buf) {
        this.header = header;
        this.buf = buf;
    }

    public void reset() {
        currentGroup = -1;
        currentGroupBits = 0;
        currentVal = 0;
    }

    /**
     * Initializes the next group, for n bits.
     * @return whether the next group exists
     */
    public boolean nextGroup(final int nBits) {
        currentGroup++;
        if (currentGroup >= header.length * 2) {
            LOG.error("Header too small for group {}", currentGroup);
            // We're now in an error state, but we'll consume so the buffer advances and we avoid an
            // infinite loop
            consume(nBits);
            return false;
        }

        if ((getCurrentNibble() & 8) == 0) {
            // group does not exist, return and do not consume anything from the buffer
            return false;
        }

        currentGroupBits = nBits;
        currentVal = consume(nBits);

        return (getCurrentNibble() & 8) != 0;
    }

    private int consume(final int nBits) {
        switch (nBits) {
            case 8:
                return buf.get() & 0xff;
            case 16:
                return buf.getShort() & 0xffff;
            case 32:
                return buf.getInt();
        }

        throw new IllegalArgumentException("Unsupported number of bits " + nBits);
    }

    private int getCurrentNibble() {
        final int headerByte = currentGroup / 2;
        if (currentGroup % 2 == 0) {
            return (header[headerByte] & 0xf0) >> 4;
        } else {
            return header[headerByte] & 0x0f;
        }
    }

    public boolean hasFirst() {
        return isValid(0);
    }

    public boolean hasSecond() {
        return isValid(1);
    }

    public boolean hasThird() {
        return isValid(2);
    }

    public boolean isValid(final int idx) {
        if (idx < 0 || idx > 2) {
            throw new IllegalArgumentException("Invalid idx " + idx);
        }

        return (getCurrentNibble() & (1 << (2 - idx))) != 0;
    }

    public int get(final int idx, final int nBits) {
        final int shift = currentGroupBits - idx - nBits;
        return (currentVal & (((1 << nBits) - 1) << shift)) >>> shift;
    }
}

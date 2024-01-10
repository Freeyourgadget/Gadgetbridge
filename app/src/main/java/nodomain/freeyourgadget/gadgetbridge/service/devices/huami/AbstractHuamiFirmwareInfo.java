/*  Copyright (C) 2022-2024 José Rebelo

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public abstract class AbstractHuamiFirmwareInfo {
    private byte[] bytes;

    private int crc16;
    private int crc32;

    protected HuamiFirmwareType firmwareType;

    public AbstractHuamiFirmwareInfo(byte[] bytes) {
        setBytes(bytes);
        this.firmwareType = determineFirmwareType(bytes);
    }

    public boolean isHeaderValid() {
        return getFirmwareType() != HuamiFirmwareType.INVALID;
    }

    public void checkValid() throws IllegalArgumentException {
    }

    public int[] getWhitelistedVersions() {
        return ArrayUtils.toIntArray(getCrcMap().keySet());
    }

    /**
     * @return the size of the firmware in number of bytes.
     */
    public int getSize() {
        return bytes.length;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getCrc16() {
        return crc16;
    }

    public int getCrc32() {
        return crc32;
    }

    public void setBytes(final byte[] bytes) {
        this.bytes = bytes;
        this.crc16 = CheckSums.getCRC16(bytes);
        this.crc32 = CheckSums.getCRC32(bytes);
    }

    public int getFirmwareVersion() {
        return getCrc16(); // HACK until we know how to determine the version from the fw bytes
    }

    public HuamiFirmwareType getFirmwareType() {
        return firmwareType;
    }

    public void unsetFwBytes() {
        this.bytes = null;
    }

    @Nullable
    public Bitmap getPreview() {
        return null;
    }

    public abstract String toVersion(int crc16);

    public abstract boolean isGenerallyCompatibleWith(GBDevice device);

    protected abstract Map<Integer, String> getCrcMap();

    protected abstract HuamiFirmwareType determineFirmwareType(byte[] bytes);

    public static boolean searchString32BitAligned(byte[] fwbytes, String findString) {
        ByteBuffer stringBuf = ByteBuffer.wrap((findString + "\0").getBytes());
        stringBuf.order(ByteOrder.BIG_ENDIAN);
        int[] findArray = new int[stringBuf.remaining() / 4];
        for (int i = 0; i < findArray.length; i++) {
            findArray[i] = stringBuf.getInt();
        }

        ByteBuffer buf = ByteBuffer.wrap(fwbytes);
        buf.order(ByteOrder.BIG_ENDIAN);
        while (buf.remaining() > 3) {
            int arrayPos = 0;
            while (arrayPos < findArray.length && buf.remaining() > 3 && (buf.getInt() == findArray[arrayPos])) {
                arrayPos++;
            }
            if (arrayPos == findArray.length) {
                return true;
            }
        }
        return false;
    }
}

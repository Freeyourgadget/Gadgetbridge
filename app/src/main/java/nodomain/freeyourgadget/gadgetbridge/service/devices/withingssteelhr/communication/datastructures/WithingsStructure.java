/*  Copyright (C) 2021 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.Message;

/**
 * This abstract class is the common denominator for all data structures used inside commands and the corresponding responses.
 * @see Message
 */
public abstract class WithingsStructure {
    protected final static short HEADER_SIZE = 4;

    /**
     * Some messages have some end bytes, some have not.
     * Subclasses that need to have the eom appended need to overwrite this class and return true.
     * The default value is false.
     *
     * @return true if some end of message should be appended
     */
    public boolean withEndOfMessage() {
        return false;
    }

    public byte[] getRawData() {
        short length = (getLength());
        ByteBuffer rawDataBuffer = ByteBuffer.allocate(length);
        rawDataBuffer.putShort(getType());
        rawDataBuffer.putShort((short)(length - HEADER_SIZE));
        fillinTypeSpecificData(rawDataBuffer);
        return rawDataBuffer.array();
    }

    protected void addStringAsBytesWithLengthByte(ByteBuffer buffer, String str) {
        if (str == null) {
            buffer.put((byte)0);
        } else {
            byte[] stringAsBytes = str.getBytes(StandardCharsets.UTF_8);
            buffer.put((byte)stringAsBytes.length);
            buffer.put(stringAsBytes);
        }
    }

    protected void fillFromRawData(byte[] rawData) {
        fillFromRawDataAsBuffer(ByteBuffer.wrap(rawData));
    };

    protected void fillFromRawDataAsBuffer(ByteBuffer rawDataBuffer) {};

    public abstract short getLength();

    protected abstract void fillinTypeSpecificData(ByteBuffer buffer);
    public abstract short getType();

    protected String getNextString(ByteBuffer byteBuffer) {
        // For strings in the raw data the first byte of the data is the length of the string:
        int stringLength = (short)(byteBuffer.get() & 255);
        byte[] stringBytes = new byte[stringLength];
        byteBuffer.get(stringBytes);
        return new String(stringBytes, Charset.forName("UTF-8"));
    }

    protected byte[] getNextByteArray(ByteBuffer byteBuffer) {
        int arrayLength = (short)(byteBuffer.get() & 255);
        byte[] nextByteArray = new byte[arrayLength];
        byteBuffer.get(nextByteArray);
        return nextByteArray;
    }

    protected int[] getNextIntArray(ByteBuffer byteBuffer) {
        int arrayLength = (short)(byteBuffer.get() & 255);
        int[] nextIntArray = new int[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            nextIntArray[i] = byteBuffer.getInt();
        }
        return nextIntArray;
    }

    protected void addByteArrayWithLengthByte(ByteBuffer buffer, byte[] data) {
        buffer.put((byte) data.length);
        if (data.length != 0) {
            buffer.put(data);
        }
    }
}

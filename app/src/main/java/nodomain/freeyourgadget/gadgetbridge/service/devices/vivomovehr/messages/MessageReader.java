/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;

import java.nio.charset.StandardCharsets;

public class MessageReader {
    private final byte[] data;
    private int position;

    public MessageReader(byte[] data) {
        this.data = data;
    }

    public MessageReader(byte[] data, int skipOffset) {
        this.data = data;
        this.position = skipOffset;
    }

    public boolean isEof() {
        return position >= data.length;
    }

    public int getPosition() {
        return position;
    }

    public void skip(int offset) {
        if (position + offset > data.length) throw new IllegalStateException();
        position += offset;
    }

    public int readByte() {
        if (position + 1 > data.length) throw new IllegalStateException();
        final int result = BLETypeConversions.toUnsigned(data, position);
        ++position;
        return result;
    }

    public int readShort() {
        if (position + 2 > data.length) throw new IllegalStateException();
        final int result = BLETypeConversions.toUint16(data, position);
        position += 2;
        return result;
    }

    public int readInt() {
        if (position + 4 > data.length) throw new IllegalStateException();
        final int result = BLETypeConversions.toUint32(data, position);
        position += 4;
        return result;
    }

    public long readLong() {
        if (position + 8 > data.length) throw new IllegalStateException();
        final long result = BLETypeConversions.toUint64(data, position);
        position += 8;
        return result;
    }

    public String readString() {
        final int size = readByte();
        if (position + size > data.length) throw new IllegalStateException();
        final String result = new String(data, position, size, StandardCharsets.UTF_8);
        position += size;
        return result;
    }

    public byte[] readBytes(int size) {
        if (position + size > data.length) throw new IllegalStateException();
        final byte[] result = new byte[size];
        System.arraycopy(data, position, result, 0, size);
        position += size;
        return result;
    }

    public byte[] readBytesTo(int size, byte[] buffer, int offset) {
        if (offset + size > buffer.length) throw new IllegalArgumentException();
        if (position + size > data.length) throw new IllegalStateException();
        System.arraycopy(data, position, buffer, offset, size);
        position += size;
        return buffer;
    }
}

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
import java.util.Arrays;

public class MessageWriter {
    private static final int DEFAULT_BUFFER_SIZE = 16384;

    private final byte[] buffer;
    private int position;

    public MessageWriter() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public MessageWriter(int bufferSize) {
        this.buffer = new byte[bufferSize];
    }

    public void writeByte(int value) {
        if (position + 1 > buffer.length) throw new IllegalStateException();
        BLETypeConversions.writeUint8(buffer, position, value);
        ++position;
    }

    public void writeShort(int value) {
        if (position + 2 > buffer.length) throw new IllegalStateException();
        BLETypeConversions.writeUint16(buffer, position, value);
        position += 2;
    }

    public void writeInt(int value) {
        if (position + 4 > buffer.length) throw new IllegalStateException();
        BLETypeConversions.writeUint32(buffer, position, value);
        position += 4;
    }

    public void writeLong(long value) {
        if (position + 8 > buffer.length) throw new IllegalStateException();
        BLETypeConversions.writeUint64(buffer, position, value);
        position += 8;
    }

    public void writeString(String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final int size = bytes.length;
        if (size > 255) throw new IllegalArgumentException("Too long string");
        if (position + 1 + size > buffer.length) throw new IllegalStateException();
        writeByte(size);
        System.arraycopy(bytes, 0, buffer, position, size);
        position += size;
    }

    public byte[] getBytes() {
        return position == buffer.length ? buffer : Arrays.copyOf(buffer, position);
    }

    public byte[] peekBytes() {
        return buffer;
    }

    public int getSize() {
        return position;
    }

    public void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeBytes(byte[] bytes, int offset, int size) {
        if (position + size > buffer.length) throw new IllegalStateException();
        System.arraycopy(bytes, offset, buffer, position, size);
        position += size;
    }
}

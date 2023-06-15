package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;

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
        BinaryUtils.writeByte(buffer, position, value);
        ++position;
    }

    public void writeShort(int value) {
        if (position + 2 > buffer.length) throw new IllegalStateException();
        BinaryUtils.writeShort(buffer, position, value);
        position += 2;
    }

    public void writeInt(int value) {
        if (position + 4 > buffer.length) throw new IllegalStateException();
        BinaryUtils.writeInt(buffer, position, value);
        position += 4;
    }

    public void writeLong(long value) {
        if (position + 8 > buffer.length) throw new IllegalStateException();
        BinaryUtils.writeLong(buffer, position, value);
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

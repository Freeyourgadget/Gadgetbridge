package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MessageWriter {
    private static final int DEFAULT_BUFFER_SIZE = 16384;
    private final ByteBuffer byteBuffer;

    public MessageWriter() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public MessageWriter(int bufferSize) {
        this.byteBuffer = ByteBuffer.allocate(bufferSize);
        this.byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public MessageWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        this.byteBuffer.clear();
        this.byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteBuffer.order(byteOrder);
    }

    public void writeByte(int value) {
        byteBuffer.put((byte) value);
    }

    public void writeShort(int value) {
        byteBuffer.putShort((short) value);
    }

    public void writeInt(int value) {
        byteBuffer.putInt(value);
    }

    public void writeLong(long value) {
        byteBuffer.putLong(value);
    }

    public void writeFloat32(float value) {
        byteBuffer.putFloat(value);
    }

    public void writeFloat64(double value) {
        byteBuffer.putDouble(value);
    }

    public void writeString(String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final int size = bytes.length;
        if (size > 255) throw new IllegalArgumentException("Too long string");

        byteBuffer.put((byte) size);
        byteBuffer.put(bytes);
    }

    public byte[] getBytes() {
        //TODO: implement the correct flip()/compat() logic
        return byteBuffer.hasRemaining() ? Arrays.copyOf(byteBuffer.array(), byteBuffer.position()) : byteBuffer.array();
    }

    public byte[] peekBytes() {
        return byteBuffer.array();
    }

    public int getSize() {
        return byteBuffer.position();
    }

    public int getLimit() {
        return byteBuffer.limit();
    }

    public void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeBytes(byte[] bytes, int offset, int size) {
        byteBuffer.put(Arrays.copyOfRange(bytes, offset, offset + size));
    }
}

package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class GarminByteBufferReader {
    protected final ByteBuffer byteBuffer;

    public GarminByteBufferReader(byte[] data) {
        this.byteBuffer = ByteBuffer.wrap(data);
    }

    public ByteBuffer asReadOnlyBuffer() {
        return byteBuffer.asReadOnlyBuffer();
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteBuffer.order(byteOrder);
    }

    public int readByte() {
        if (!byteBuffer.hasRemaining()) throw new IllegalStateException();

        return Byte.toUnsignedInt(byteBuffer.get());
    }

    public int getPosition() {
        return byteBuffer.position();
    }

    public int readShort() {
        if (byteBuffer.remaining() < 2) throw new IllegalStateException();

        return Short.toUnsignedInt(byteBuffer.getShort());
    }

    public int readInt() {
        if (byteBuffer.remaining() < 4) throw new IllegalStateException();

        return byteBuffer.getInt();
    }

    public long readLong() {
        if (byteBuffer.remaining() < 8) throw new IllegalStateException();

        return byteBuffer.getLong();
    }

    public float readFloat32() {
        if (byteBuffer.remaining() < 4) throw new IllegalStateException();

        return byteBuffer.getFloat();
    }

    public double readFloat64() {
        if (byteBuffer.remaining() < 8) throw new IllegalStateException();

        return byteBuffer.getDouble();
    }

    public String readString() {
        final int size = readByte();
        byte[] bytes = new byte[size];
        if (byteBuffer.remaining() < size) throw new IllegalStateException();
        byteBuffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readBytes(int size) {
        byte[] bytes = new byte[size];

        if (byteBuffer.remaining() < size) throw new IllegalStateException();
        byteBuffer.get(bytes);

        return bytes;
    }

}

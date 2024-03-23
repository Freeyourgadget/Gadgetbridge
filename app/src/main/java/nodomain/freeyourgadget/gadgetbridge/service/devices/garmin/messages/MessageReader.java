package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class MessageReader {
    protected static final Logger LOG = LoggerFactory.getLogger(MessageReader.class);

    private final ByteBuffer byteBuffer;
    private final int payloadSize;

    public MessageReader(byte[] data) {
        this.byteBuffer = ByteBuffer.wrap(data);
        this.byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.payloadSize = readShort();

        checkSize();
        checkCRC();
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteBuffer.order(byteOrder);
    }

    public boolean isEof() {
        return !byteBuffer.hasRemaining();
    }

    public boolean isEndOfPayload() {
        return byteBuffer.position() >= payloadSize - 2;
    }

    public int getPosition() {
        return byteBuffer.position();
    }

    public void skip(int offset) {
        if (byteBuffer.remaining() < offset) throw new IllegalStateException();
        byteBuffer.position(byteBuffer.position() + offset);
    }

    public int readByte() {
        if (!byteBuffer.hasRemaining()) throw new IllegalStateException();

        return Byte.toUnsignedInt(byteBuffer.get());
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

    private int getCapacity() {
        return byteBuffer.capacity();
    }

    private void checkSize() {
        if (payloadSize > getCapacity()) {
            LOG.error("Received GFDI packet with invalid length: {} vs {}", payloadSize, getCapacity());
            throw new IllegalArgumentException("Received GFDI packet with invalid length");
        }
    }

    private void checkCRC() {
        final int crc = Short.toUnsignedInt(byteBuffer.getShort(payloadSize - 2));
        final int correctCrc = ChecksumCalculator.computeCrc(byteBuffer.asReadOnlyBuffer(), 0, payloadSize - 2);
        if (crc != correctCrc) {
            LOG.error("Received GFDI packet with invalid CRC: {} vs {}", crc, correctCrc);
            throw new IllegalArgumentException("Received GFDI packet with invalid CRC");
        }
    }

    public void warnIfLeftover() {
        if (byteBuffer.hasRemaining() && byteBuffer.position() < (byteBuffer.limit() - 2)) {
            int pos = byteBuffer.position();
            int numBytes = (byteBuffer.limit() - 2) - byteBuffer.position();
            byte[] leftover = new byte[numBytes];
            byteBuffer.get(leftover);
            byteBuffer.position(pos);
            LOG.warn("Leftover bytes when parsing message. Bytes: {}, complete message: {}", GB.hexdump(leftover), GB.hexdump(byteBuffer.array()));
        }
    }
}
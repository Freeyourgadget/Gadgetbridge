package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator;

import java.nio.ByteBuffer;

public class CobsCoDec {
    private static final long BUFFER_TIMEOUT = 1500L; // turn this value up while debugging
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(1000);
    private long lastUpdate;
    private byte[] cobsDecodedMessage;

    /**
     * Accumulates received bytes in a local buffer, clearing it after a timeout, and attempts to
     * parse it.
     *
     * @param bytes
     */
    public void receivedBytes(byte[] bytes) {
        final long now = System.currentTimeMillis();
        if ((now - lastUpdate) > BUFFER_TIMEOUT) {
            reset();
        }
        lastUpdate = now;

        byteBuffer.put(bytes);
        decode();
    }

    private void reset() {
        cobsDecodedMessage = null;
        byteBuffer.clear();
    }

    public byte[] retrieveMessage() {
        final byte[] resultPacket = cobsDecodedMessage;
        cobsDecodedMessage = null;
        return resultPacket;
    }


    /**
     * COBS decoding algorithm variant, which relies on a leading and a trailing 0 byte (the former
     * is not part of default implementations).
     * This function removes the complete message from the internal buffer, if it could be decoded.
     */
    private void decode() {
        if (cobsDecodedMessage != null) {
            // packet is waiting, unable to parse more
            return;
        }
        if (byteBuffer.position() < 4) {
            // minimal payload length including the padding
            return;
        }
        if (0 != byteBuffer.get(byteBuffer.position() - 1))
            return; //no 0x00 at the end, hence no full packet
        byteBuffer.position(byteBuffer.position() - 1); //don't process the trailing 0
        byteBuffer.flip();
        if (0 != byteBuffer.get())
            return; //no 0x00 at the start
        ByteBuffer decodedBytesBuffer = ByteBuffer.allocate(byteBuffer.limit()); //leading and trailing 0x00 bytes
        while (byteBuffer.hasRemaining()) {
            byte code = byteBuffer.get();
            if (code == 0) {
                break;
            }
            int codeValue = code & 0xFF;
            int payloadSize = codeValue - 1;
            for (int i = 0; i < payloadSize; i++) {
                decodedBytesBuffer.put(byteBuffer.get());
            }
            if (codeValue != 0xFF && byteBuffer.hasRemaining()) {
                decodedBytesBuffer.put((byte) 0); // Append a zero byte after the payload
            }
        }

        decodedBytesBuffer.flip();
        cobsDecodedMessage = new byte[decodedBytesBuffer.remaining()];
        decodedBytesBuffer.get(cobsDecodedMessage);
        byteBuffer.compact();
    }

    // this implementation of COBS relies on a leading and a trailing 0 byte (the former is not part of default implementations)
    public byte[] encode(byte[] data) {
        ByteBuffer encodedBytesBuffer = ByteBuffer.allocate((data.length * 2) + 1); // Maximum expansion

        encodedBytesBuffer.put((byte) 0);// Garmin initial padding
        ByteBuffer buffer = ByteBuffer.wrap(data);

        while (buffer.position() < buffer.limit()) {
            int startPos = buffer.position();
            int zeroIndex = buffer.position();

            while (buffer.hasRemaining() && buffer.get() != 0) {
                zeroIndex++;
            }

            int payloadSize = zeroIndex - startPos;

            while (payloadSize > 0xFE) {
                encodedBytesBuffer.put((byte) 0xFF); // Maximum payload size indicator
                for (int i = 0; i < 0xFE; i++) {
                    encodedBytesBuffer.put(data[startPos + i]);
                }
                payloadSize -= 0xFE;
                startPos += 0xFE;
            }

            encodedBytesBuffer.put((byte) (payloadSize + 1));

            for (int i = startPos; i < zeroIndex; i++) {
                encodedBytesBuffer.put(data[i]);
            }

            if (buffer.hasRemaining()) {
                zeroIndex++; // Include the zero byte in the next block
            }

            if (!buffer.hasRemaining() && payloadSize == 0) {
                break;
            }

            buffer.position(zeroIndex);
        }

        encodedBytesBuffer.put((byte) 0); // Append a zero byte to indicate end of encoding
        encodedBytesBuffer.flip();

        byte[] encodedBytes = new byte[encodedBytesBuffer.remaining()];
        encodedBytesBuffer.get(encodedBytes);

        return encodedBytes;
    }

}

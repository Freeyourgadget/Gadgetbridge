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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CmfCharacteristic {
    private final Logger LOG = LoggerFactory.getLogger(CmfCharacteristic.class);

    private static final byte[] AES_IV = new byte[]{0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x5a};
    private static final byte PAYLOAD_HEADER = (byte) 0xf5;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final UUID characteristicUUID;

    private final Handler handler;

    private byte[] sessionKey;

    private int mtu = 247;

    private final Map<CmfCommand, ChunkBuffer> chunkBuffers = new HashMap<>();

    public CmfCharacteristic(final BluetoothGattCharacteristic bluetoothGattCharacteristic,
                             final Handler handler) {
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.characteristicUUID = bluetoothGattCharacteristic.getUuid();
        this.handler = handler;
    }

    public UUID getCharacteristicUUID() {
        return characteristicUUID;
    }

    public void setSessionKey(final byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void setMtu(final int mtu) {
        this.mtu = mtu;
    }

    public void sendCommand(final TransactionBuilder builder, final CmfCommand cmd, final byte[] payload) {
        final byte[][] chunks;

        final boolean encrypted = shouldEncrypt(cmd);
        if (encrypted) {
            chunks = makeChunksEncrypted(payload);
        } else {
            chunks = makeChunksPlaintext(payload);
        }

        if (chunks == null) {
            // Something went wrong chunking - error was already printed
            return;
        }

        LOG.debug(
                "Send command: cmd={}{}",
                cmd,
                payload.length > 0 ? " payload=" + GB.hexdump(payload) : ""
        );

        for (int i = 0; i < chunks.length; i++) {
            final byte[] chunk = chunks[i];

            final ByteBuffer buf = ByteBuffer.allocate(chunk.length + 11).order(ByteOrder.BIG_ENDIAN);
            buf.put(PAYLOAD_HEADER);
            buf.putShort((short) chunk.length);
            buf.putShort((short) cmd.getCmd1());
            buf.putShort((short) chunks.length);
            buf.putShort((short) (i + 1));
            buf.putShort((short) cmd.getCmd2());
            buf.put(chunk);

            builder.write(bluetoothGattCharacteristic, buf.array());
        }
    }

    private byte[][] makeChunksPlaintext(final byte[] payload) {
        final int chunkSize = mtu - 20;
        final int numChunks = (int) Math.ceil(payload.length / (float) chunkSize);
        final byte[][] chunks = new byte[numChunks][];

        final CRC32 crc = new CRC32();

        for (int i = 0; i < numChunks; i++) {
            final int startIdx = i * chunkSize;
            final int endIdx = Math.min(startIdx + chunkSize, payload.length);
            final byte[] chunk = ArrayUtils.subarray(payload, startIdx, endIdx);

            crc.reset();
            crc.update(chunk, 0, chunk.length);

            chunks[i] = ArrayUtils.addAll(
                    chunk,
                    BLETypeConversions.fromUint32((int) crc.getValue())
            );
        }

        return chunks;
    }

    @Nullable
    private byte[][] makeChunksEncrypted(final byte[] payload) {
        if (payload.length == 0) {
            return new byte[1][0];
        }

        // AES will output 16-byte blocks, exclude the protocol overhead (11 bytes)
        final int maxEncryptedPayloadSize = ((mtu - 11) / 16) * 16;
        final int maxPayloadSize = maxEncryptedPayloadSize - 4 - 1; // exclude 4 bytes for crc and 1 byte of aes padding
        final int numChunks = (int) Math.ceil(payload.length / (float) (maxPayloadSize));
        final byte[][] chunks = new byte[numChunks][];

        if (numChunks != 1) {
            LOG.debug("Splitting payload into {} chunks of {} bytes", numChunks, maxPayloadSize);
        }

        final CRC32 crc = new CRC32();

        for (int i = 0; i < numChunks; i++) {
            final int startIdx = i * maxPayloadSize;
            final int endIdx = Math.min(startIdx + maxPayloadSize, payload.length);
            final byte[] chunk = ArrayUtils.subarray(payload, startIdx, endIdx);

            crc.reset();
            crc.update(chunk, 0, chunk.length);

            final byte[] payloadToEncrypt = ArrayUtils.addAll(
                    chunk,
                    BLETypeConversions.fromUint32((int) crc.getValue())
            );

            try {
                chunks[i] = CryptoUtils.encryptAES_CBC_Pad(payloadToEncrypt, sessionKey, AES_IV);
            } catch (final GeneralSecurityException e) {
                LOG.error("Failed to encrypt chunk", e);
                return null;
            }
        }

        return chunks;
    }

    private boolean shouldEncrypt(final CmfCommand cmd) {
        switch (cmd) {
            case AUTH_PAIR_REQUEST:
            case AUTH_PAIR_REPLY:
            case DATA_CHUNK_WRITE_AGPS:
            case DATA_CHUNK_WRITE_WATCHFACE:
                return false;
        }

        return true;
    }

    public void onCharacteristicChanged(final byte[] value) {
        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN);

        final byte header = buf.get();
        if (header != PAYLOAD_HEADER) {
            LOG.error("Unexpected first byte {}", String.format("0x%02x", header));
            return;
        }

        final int payloadLength = buf.getShort();
        final int cmd1 = buf.getShort() & 0xFFFF;
        final int chunkCount = buf.getShort();
        final int chunkIndex = buf.getShort();
        final int cmd2 = buf.getShort() & 0xFFFF;

        final CmfCommand cmd = CmfCommand.fromCodes(cmd1, cmd2);

        final byte[] payload;
        if (payloadLength > 0) {
            try {
                if (cmd == null || shouldEncrypt(cmd)) {
                    final byte[] encryptedPayload = new byte[payloadLength];
                    buf.get(encryptedPayload);

                    final byte[] decryptedPayload = CryptoUtils.decryptAES_CBC_Pad(encryptedPayload, sessionKey, AES_IV);
                    payload = ArrayUtils.subarray(decryptedPayload, 0, decryptedPayload.length - 4);
                    final int expectedCrc = BLETypeConversions.toUint32(decryptedPayload, decryptedPayload.length - 4);
                    final CRC32 crc = new CRC32();
                    crc.update(payload, 0, payload.length);
                    final int actualCrc = (int) crc.getValue();
                    if (actualCrc != expectedCrc) {
                        LOG.error("Payload CRC mismatch for {}: got {}, expected {}", cmd, String.format("%08X", actualCrc), String.format("%08X", expectedCrc));
                        if (chunkCount > 1) {
                            chunkBuffers.remove(cmd);
                        }
                        return;
                    }
                } else {
                    // Plaintext payload - it does not have the crc, but the length still includes it (?)
                    payload = new byte[buf.limit() - buf.position()];
                    buf.get(payload);
                }
            } catch (final GeneralSecurityException e) {
                LOG.error("Failed to decrypt payload for {} ({}/{})", cmd, String.format("0x%04x", cmd1), String.format("0x%04x", cmd2), e);
                if (cmd == CmfCommand.AUTH_FAILED) {
                    handler.onCommand(cmd, new byte[0]);
                }
                if (chunkCount > 1) {
                    chunkBuffers.remove(cmd);
                }
                return;
            }
        } else {
            payload = new byte[0];
        }

        LOG.debug(
                "Got {}: {}{}",
                chunkCount > 1 ? String.format(Locale.ROOT, "chunk %d/%d", chunkIndex, chunkCount) : "command",
                cmd != null ? String.format("cmd=%s", cmd) : String.format("cmd1=0x%04x cmd2=0x%04x", cmd1, cmd2),
                payload.length > 0 ? " payload=" + GB.hexdump(payload) : ""
        );

        if (cmd == null) {
            // Just ignore unknown commands
            LOG.warn("Unknown command cmd1={} cmd2={}", String.format("0x%04x", cmd1), String.format("0x%04x", cmd2));
            return;
        }

        final byte[] fullPayload;
        if (chunkCount == 1) {
            // Single-chunk payload - just pass it through
            fullPayload = payload;
        } else {
            final ChunkBuffer buffer;
            if (chunkBuffers.containsKey(cmd)) {
                buffer = Objects.requireNonNull(chunkBuffers.get(cmd));
            } else {
                buffer = new ChunkBuffer();
                chunkBuffers.put(cmd, buffer);
            }

            if (chunkIndex != buffer.expectedChunk) {
                LOG.warn("Got unexpected chunk, expected {}", buffer.expectedChunk);

                if (chunkIndex != 1) {
                    // This chunk is not the first one and we got out of sync - ignore it and do not proceed
                    return;
                }

                // Just discard whatever we had and start over
                buffer.baos.reset();
            }

            try {
                buffer.baos.write(payload);
            } catch (final IOException e) {
                LOG.error("Failed to write payload to chunk buffer", e);
                return;
            }

            buffer.expectedChunk = chunkIndex + 1;

            if (chunkIndex != chunkCount) {
                // Chunk buffer not full yet
                return;
            }

            LOG.debug("Got all {} chunks for {}", chunkCount, cmd);

            fullPayload = buffer.baos.toByteArray().clone();
            chunkBuffers.remove(cmd);
        }

        if (handler == null) {
            LOG.error("Handler is null for {}", characteristicUUID);
            return;
        }

        try {
            handler.onCommand(cmd, fullPayload);
        } catch (final Exception e) {
            LOG.error("Exception while handling command", e);
        }
    }

    public interface Handler {
        void onCommand(CmfCommand cmd, byte[] payload);
    }

    private static class ChunkBuffer {
        private int expectedChunk = 1;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    }
}

/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;


import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public class XiaomiCharacteristic {
    public static final byte[] PAYLOAD_ACK = new byte[]{0, 0, 3, 0};
    public static final byte[] PAYLOAD_CHUNKED_START_ACK = new byte[]{0, 0, 1, 1};
    public static final byte[] PAYLOAD_CHUNKED_END_ACK = new byte[]{0, 0, 1, 0};

    private final Logger LOG;

    private final XiaomiSupport mSupport;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final UUID characteristicUUID;

    // Encryption
    private XiaomiAuthService authService = null;
    private boolean isEncrypted = false;
    private short encryptedIndex = 0;

    // Chunking
    private int numChunks = 0;
    private int currentChunk = 0;
    private final ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();

    // Scheduling
    // TODO timeouts
    private final Queue<byte[]> payloadQueue = new LinkedList<>();
    private boolean waitingAck = false;
    private boolean sendingChunked = false;
    private byte[] currentSending = null;

    private Handler handler = null;

    public XiaomiCharacteristic(final XiaomiSupport support,
                                final BluetoothGattCharacteristic bluetoothGattCharacteristic,
                                @Nullable final XiaomiAuthService authService) {
        this.mSupport = support;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.authService = authService;
        this.isEncrypted = authService != null;
        this.LOG = LoggerFactory.getLogger("XiaomiCharacteristic [" + bluetoothGattCharacteristic.getUuid().toString() + "]");
        this.characteristicUUID = bluetoothGattCharacteristic.getUuid();
    }

    public UUID getCharacteristicUUID() {
        return characteristicUUID;
    }

    public void setHandler(final Handler handler) {
        this.handler = handler;
    }

    public void setEncrypted(final boolean encrypted) {
        this.isEncrypted = encrypted;
    }

    public void reset() {
        this.numChunks = 0;
        this.currentChunk = 0;
        this.encryptedIndex = 1; // 0 is used by auth service
        this.chunkBuffer.reset();
        this.payloadQueue.clear();
        this.waitingAck = false;
        this.sendingChunked = false;
        this.currentSending = null;
    }

    /**
     * Write bytes to this characteristic, encrypting and splitting it into chunks if necessary.
     */
    public void write(final byte[] value) {
        payloadQueue.add(value);
        sendNext();
    }

    /**
     * Write bytes to this characteristic directly.
     */
    public void writeDirect(final TransactionBuilder builder, final byte[] value) {
        builder.write(bluetoothGattCharacteristic, value);
    }

    public void onCharacteristicChanged(final byte[] value) {
        if (Arrays.equals(value, PAYLOAD_ACK)) {
            LOG.debug("Got ack");
            currentSending = null;
            waitingAck = false;
            sendNext();
            return;
        }

        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        final int chunk = buf.getShort();
        if (chunk != 0) {
            // Chunked packet
            final byte[] chunkBytes = new byte[buf.limit() - buf.position()];
            buf.get(chunkBytes);
            try {
                chunkBuffer.write(chunkBytes);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            currentChunk++;
            LOG.debug("Got chunk {} of {}", currentChunk, numChunks);
            if (chunk == numChunks) {
                sendChunkEndAck();

                if (authService != null) {
                    // chunks are always encrypted if an auth service is available
                    handler.handle(authService.decrypt(chunkBuffer.toByteArray()));
                } else {
                    handler.handle(chunkBuffer.toByteArray());
                }
            }
        } else {
            // Not a chunk / single-packet
            final byte type = buf.get();

            switch (type) {
                case 0:
                    // Chunked start request
                    final byte one = buf.get(); // ?
                    if (one != 1) {
                        LOG.warn("Chunked start request: expected 1, got {}", one);
                        return;
                    }
                    numChunks = buf.getShort();
                    LOG.debug("Got chunked start request for {} chunks", numChunks);
                    sendChunkStartAck();
                    return;
                case 1:
                    // Chunked ack
                    final byte subtype = buf.get();
                    switch (subtype) {
                        case 0:
                            LOG.debug("Got chunked ack end");
                            currentSending = null;
                            sendingChunked = false;
                            sendNext();
                            return;
                        case 1:
                            LOG.debug("Got chunked ack start");
                            final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunks");
                            for (int i = 0; i * 242 < currentSending.length; i ++) {
                                final int startIndex = i * 242;
                                final int endIndex = Math.min((i + 1) * 242, currentSending.length);
                                LOG.debug("Sending chunk {} from {} to {}", i, startIndex, endIndex);
                                final byte[] chunkToSend = new byte[2 + endIndex - startIndex];
                                BLETypeConversions.writeUint16(chunkToSend, 0, i + 1);
                                System.arraycopy(currentSending, startIndex, chunkToSend, 2, endIndex - startIndex);
                                builder.write(bluetoothGattCharacteristic, chunkToSend);
                            }

                            builder.queue(mSupport.getQueue());
                            return;
                    }

                    LOG.warn("Unknown chunked ack subtype {}", subtype);

                    return;
                case 2:
                    // Single command
                    sendAck();

                    final byte encryption = buf.get();
                    final byte[] plainValue;
                    if (encryption == 1) {
                        final byte[] encryptedValue = new byte[buf.limit() - buf.position()];
                        buf.get(encryptedValue);
                        plainValue = authService.decrypt(encryptedValue);
                    } else {
                        plainValue = new byte[buf.limit() - buf.position()];
                        buf.get(plainValue);
                    }

                    handler.handle(plainValue);

                    return;
                case 3:
                    // ack
                    LOG.debug("Got ack");
            }
        }
    }

    private void sendNext() {
        if (waitingAck || sendingChunked) {
            LOG.debug("Already sending something");
            return;
        }

        final byte[] payload = payloadQueue.poll();
        if (payload == null) {
            LOG.debug("Nothing to send");
            return;
        }

        if (isEncrypted) {
            currentSending = authService.encrypt(payload, encryptedIndex);
        } else {
            currentSending = payload;
        }

        if (shouldWriteChunked(currentSending)) {
            LOG.debug("Sending next - chunked");
            // FIXME this is not efficient - re-encrypt with the correct key for chunked (assumes
            //  final encrypted size is the same - need to check)
            if (isEncrypted) {
                currentSending = authService.encrypt(payload, (short) 0);
            }

            sendingChunked = true;

            final ByteBuffer buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) 0);
            buf.put((byte) 0);
            buf.put((byte) 1);
            buf.putShort((short) Math.round(currentSending.length / 247.0));

            final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunked start");
            builder.write(bluetoothGattCharacteristic, buf.array());
            builder.queue(mSupport.getQueue());
        } else {
            LOG.debug("Sending next - single");

            // Encrypt single command
            final int commandLength = 6 + currentSending.length;

            final ByteBuffer buf = ByteBuffer.allocate(commandLength).order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) 0);
            buf.put((byte) 2); // 2 for command
            buf.put((byte) 1); // 1 for encrypted
            buf.putShort(encryptedIndex++);
            buf.put(currentSending); // it's already encrypted

            waitingAck = true;

            final TransactionBuilder builder = mSupport.createTransactionBuilder("send single command");
            builder.write(bluetoothGattCharacteristic, buf.array());
            builder.queue(mSupport.getQueue());
        }
    }

    private boolean shouldWriteChunked(final byte[] payload) {
        if (!isEncrypted) {
            // non-encrypted are always chunked
            return true;
        }

        return payload.length + 6 > 244;
    }

    private void sendAck() {
        final TransactionBuilder builder = mSupport.createTransactionBuilder("send ack");
        builder.write(bluetoothGattCharacteristic, PAYLOAD_ACK);
        builder.queue(mSupport.getQueue());
    }

    private void sendChunkStartAck() {
        final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunked start ack");
        builder.write(bluetoothGattCharacteristic, PAYLOAD_CHUNKED_START_ACK);
        builder.queue(mSupport.getQueue());
    }

    private void sendChunkEndAck() {
        final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunked end ack");
        builder.write(bluetoothGattCharacteristic, PAYLOAD_CHUNKED_END_ACK);
        builder.queue(mSupport.getQueue());
    }

    public interface Handler {
        void handle(final byte[] payload);
    }
}

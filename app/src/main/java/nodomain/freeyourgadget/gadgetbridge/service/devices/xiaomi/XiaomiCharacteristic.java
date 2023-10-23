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
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiCharacteristic {
    private final Logger LOG = LoggerFactory.getLogger(XiaomiCharacteristic.class);

    public static final byte[] PAYLOAD_ACK = new byte[]{0, 0, 3, 0};

    // max chunk size, including headers
    public static final int MAX_WRITE_SIZE = 242;

    private final XiaomiSupport mSupport;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final UUID characteristicUUID;

    // Encryption
    private final XiaomiAuthService authService;
    private boolean isEncrypted;
    public boolean incrementNonce = true;
    private short encryptedIndex = 0;

    // Chunking
    private int numChunks = 0;
    private int currentChunk = 0;
    private final ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();

    // Scheduling
    // TODO timeouts
    private final Queue<Payload> payloadQueue = new LinkedList<>();
    private boolean waitingAck = false;
    private boolean sendingChunked = false;
    private Payload currentPayload = null;

    private Handler handler = null;

    private SendCallback callback;

    public XiaomiCharacteristic(final XiaomiSupport support,
                                final BluetoothGattCharacteristic bluetoothGattCharacteristic,
                                @Nullable final XiaomiAuthService authService) {
        this.mSupport = support;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.authService = authService;
        this.isEncrypted = authService != null;
        this.characteristicUUID = bluetoothGattCharacteristic.getUuid();
    }

    public UUID getCharacteristicUUID() {
        return characteristicUUID;
    }

    public void setHandler(final Handler handler) {
        this.handler = handler;
    }

    public void setCallback(final SendCallback callback) {
        this.callback = callback;
    }

    public void setEncrypted(final boolean encrypted) {
        this.isEncrypted = encrypted;
    }

    public void setIncrementNonce(final boolean incrementNonce) {
        this.incrementNonce = incrementNonce;
    }

    public void reset() {
        this.numChunks = 0;
        this.currentChunk = 0;
        this.encryptedIndex = 1; // 0 is used by auth service
        this.chunkBuffer.reset();
        this.payloadQueue.clear();
        this.waitingAck = false;
        this.sendingChunked = false;
        this.currentPayload = null;
    }

    /**
     * Write bytes to this characteristic, encrypting and splitting it into chunks if necessary.
     */
    public void write(final String taskName, final byte[] value) {
        write(null, new Payload(taskName, value));
    }

    /**
     * Write bytes to this characteristic, encrypting and splitting it into chunks if necessary. Uses
     * the provided if we need to schedule something, otherwise it will be queued as other commands.
     */
    public void write(final TransactionBuilder builder, final byte[] value) {
        write(builder, new Payload(builder.getTaskName(), value));
    }

    private void write(final TransactionBuilder builder, final Payload payload) {
        payloadQueue.add(payload);
        sendNext(builder);
    }

    public void onCharacteristicChanged(final byte[] value) {
        if (Arrays.equals(value, PAYLOAD_ACK)) {
            LOG.debug("Got ack");
            currentPayload = null;
            waitingAck = false;
            if (callback != null) {
                callback.onSend(payloadQueue.size());
            }
            sendNext(null);
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

                if (isEncrypted) {
                    // chunks are always encrypted if an auth service is available
                    handler.handle(authService.decrypt(chunkBuffer.toByteArray()));
                } else {
                    handler.handle(chunkBuffer.toByteArray());
                }

                currentChunk = 0;
                chunkBuffer.reset();
            }
        } else {
            // Not a chunk / single-packet
            final byte type = buf.get();

            switch (type) {
                case 0:
                    // Chunked start request
                    final byte messageEncrypted = buf.get();
                    byte expectedResult = (byte) (isEncrypted ? 1 : 0);
                    if (messageEncrypted != expectedResult) {
                        LOG.warn("Chunked start request: expected {}, got {}", expectedResult, messageEncrypted);
                        return;
                    }
                    numChunks = buf.getShort();
                    currentChunk = 0;
                    chunkBuffer.reset();
                    LOG.debug("Got chunked start request for {} chunks", numChunks);
                    sendChunkStartAck();
                    return;
                case 1:
                    // Chunked ack
                    final byte subtype = buf.get();
                    switch (subtype) {
                        case 0:
                            LOG.debug("Got chunked ack end");
                            currentPayload = null;
                            sendingChunked = false;
                            if (callback != null) {
                                callback.onSend(payloadQueue.size());
                            }
                            sendNext(null);
                            return;
                        case 1:
                            LOG.debug("Got chunked ack start");
                            final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunks for " + currentPayload.getTaskName());
                            final byte[] payload = currentPayload.getBytesToSend();
                            for (int i = 0; i * MAX_WRITE_SIZE < payload.length; i++) {
                                final int startIndex = i * MAX_WRITE_SIZE;
                                final int endIndex = Math.min((i + 1) * MAX_WRITE_SIZE, payload.length);
                                LOG.debug("Sending chunk {} from {} to {} for {}", i, startIndex, endIndex, currentPayload.getTaskName());
                                final byte[] chunkToSend = new byte[2 + endIndex - startIndex];
                                BLETypeConversions.writeUint16(chunkToSend, 0, i + 1);
                                System.arraycopy(payload, startIndex, chunkToSend, 2, endIndex - startIndex);
                                builder.write(bluetoothGattCharacteristic, chunkToSend);
                            }

                            builder.queue(mSupport.getQueue());
                            return;
                        case 2:
                            LOG.warn("Got chunked nack for {}", currentPayload.getTaskName());
                            currentPayload = null;
                            sendingChunked = false;
                            if (callback != null) {
                                callback.onSend(payloadQueue.size());
                            }
                            sendNext(null);
                            return;
                    }

                    LOG.warn("Unknown chunked ack subtype {} for {}", subtype, currentPayload.getTaskName());

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

    private void sendNext(@Nullable final TransactionBuilder b) {
        if (waitingAck || sendingChunked) {
            LOG.debug("Already sending something");
            return;
        }

        currentPayload = payloadQueue.poll();
        if (currentPayload == null) {
            LOG.debug("Nothing to send");
            return;
        }

        LOG.debug("Will send {}", GB.hexdump(currentPayload.getBytesToSend()));

        final boolean encrypt = isEncrypted && authService.isEncryptionInitialized();

        if (encrypt) {
            currentPayload.setBytesToSend(authService.encrypt(currentPayload.getBytesToSend(), incrementNonce ? encryptedIndex : 0));
        }

        if (shouldWriteChunked(currentPayload.getBytesToSend())) {
            if (encrypt && incrementNonce) {
                // Prepend encrypted index for the nonce
                currentPayload.setBytesToSend(
                        ByteBuffer.allocate(2 + currentPayload.getBytesToSend().length).order(ByteOrder.LITTLE_ENDIAN)
                                .putShort(encryptedIndex++)
                                .put(currentPayload.getBytesToSend())
                                .array()
                );
            }

            LOG.debug("Sending {} - chunked", currentPayload.getTaskName());

            sendingChunked = true;

            final ByteBuffer buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) 0);
            buf.put((byte) 0);
            buf.put((byte) (encrypt ? 1 : 0));
            buf.putShort((short) Math.ceil(currentPayload.getBytesToSend().length / (float) MAX_WRITE_SIZE));

            final TransactionBuilder builder = b == null ? mSupport.createTransactionBuilder("send chunked start for " + currentPayload.getTaskName()) : b;
            builder.write(bluetoothGattCharacteristic, buf.array());
            if (b == null) {
                builder.queue(mSupport.getQueue());
            }
        } else {
            LOG.debug("Sending {} - single", currentPayload.getTaskName());

            // Encrypt single command
            final int commandLength = (encrypt ? 6 : 4) + currentPayload.getBytesToSend().length;

            final ByteBuffer buf = ByteBuffer.allocate(commandLength).order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) 0);
            buf.put((byte) 2); // 2 for command
            buf.put((byte) (encrypt ? 1 : 2));
            if (encrypt) {
                if (incrementNonce) {
                    buf.putShort(encryptedIndex++);
                } else {
                    buf.putShort((short) 0);
                }
            }
            buf.put(currentPayload.getBytesToSend()); // it's already encrypted

            waitingAck = true;

            final TransactionBuilder builder = b == null ? mSupport.createTransactionBuilder("send single command for " + currentPayload.getTaskName()) : b;
            builder.write(bluetoothGattCharacteristic, buf.array());
            if (b == null) {
                builder.queue(mSupport.getQueue());
            }
        }
    }

    private boolean shouldWriteChunked(final byte[] payload) {
        if (!isEncrypted) {
            // non-encrypted are always chunked
            return true;
        }

        // payload + 6 bytes at the start with the encryption stuff
        return payload.length + 6 > MAX_WRITE_SIZE;
    }

    private void sendAck() {
        final TransactionBuilder builder = mSupport.createTransactionBuilder("send ack");
        builder.write(bluetoothGattCharacteristic, PAYLOAD_ACK);
        builder.queue(mSupport.getQueue());
    }

    private void sendChunkStartAck() {
        final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunked start ack");
        builder.write(bluetoothGattCharacteristic, new byte[]{0x00, 0x00, 0x01, 0x01});
        builder.queue(mSupport.getQueue());
    }

    private void sendChunkEndAck() {
        final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunked end ack");
        builder.write(bluetoothGattCharacteristic, new byte[]{0x00, 0x00, 0x01, 0x00});
        builder.queue(mSupport.getQueue());
    }

    public interface Handler {
        void handle(final byte[] payload);
    }

    private static class Payload {
        private final String taskName;
        private final byte[] bytes;

        // Bytes that will actually be sent (might be encrypted)
        private byte[] bytesToSend;

        public Payload(final String taskName, final byte[] bytes) {
            this.taskName = taskName;
            this.bytes = bytes;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setBytesToSend(final byte[] bytesToSend) {
            this.bytesToSend = bytesToSend;
        }

        public byte[] getBytesToSend() {
            return bytesToSend != null ? bytesToSend : bytes;
        }
    }

    public interface SendCallback {
        void onSend(int remaining);
    }
}

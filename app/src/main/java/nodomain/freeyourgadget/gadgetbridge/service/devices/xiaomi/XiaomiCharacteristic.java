/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;


import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiCharacteristic {
    private final Logger LOG = LoggerFactory.getLogger(XiaomiCharacteristic.class);
    private static final long TIMEOUT_TASK_DELAY = 5000L;

    public static final byte[] PAYLOAD_ACK = new byte[]{0, 0, 3, 0};

    private final XiaomiBleSupport mSupport;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final UUID characteristicUUID;

    // Encryption
    private final XiaomiAuthService authService;
    private boolean isEncrypted;
    public boolean incrementNonce = true;
    private int encryptedIndex = 0;

    // max chunk size, including headers
    private int maxWriteSize = 244; // MTU of 247 - 3 bytes for the ATT overhead (based on lowest MTU observed after increasing MTU to 512)
    private int maxWriteSizeForCurrentMessage;

    // Chunking
    private int numChunks = 0;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Map<Integer, byte[]> receivedChunks = new HashMap<>();

    // Scheduling
    // TODO timeouts
    private final Queue<Payload> payloadQueue = new LinkedList<>();
    private boolean waitingAck = false;
    private boolean sendingChunked = false;
    private Payload currentPayload = null;

    private XiaomiChannelHandler channelHandler = null;

    public XiaomiCharacteristic(final XiaomiBleSupport support,
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

    public void setChannelHandler(final XiaomiChannelHandler handler) {
        this.channelHandler = handler;
    }

    public void setEncrypted(final boolean encrypted) {
        this.isEncrypted = encrypted;
    }

    public void setIncrementNonce(final boolean incrementNonce) {
        this.incrementNonce = incrementNonce;
    }

    public void reset() {
        this.numChunks = 0;
        this.encryptedIndex = 1; // 0 is used by auth service
        this.receivedChunks.clear();
        this.payloadQueue.clear();
        this.waitingAck = false;
        this.sendingChunked = false;
        this.currentPayload = null;
        cancelTimeoutTask();
    }

    /**
     * Write bytes to this characteristic, encrypting and splitting it into chunks if necessary.
     * Callback will be notified when a (n)ack has been received by the remote device.
     */
    public void write(final String taskName, final byte[] value, final SendCallback callback) {
        write(null, new Payload(taskName, value, callback));
    }

    /**
     * Write bytes to this characteristic, encrypting and splitting it into chunks if necessary.
     */
    public void write(final String taskName, final byte[] value) {
        write(taskName, value, null);
    }

    /**
     * Write bytes to this characteristic, encrypting and splitting it into chunks if necessary. Uses
     * the provided builder if we need to schedule something, otherwise it will be queued as other
     * commands. The callback will be notified when a (n)ack has been received from the remote
     * device in response to the payload being sent.
     */
    public void write(final TransactionBuilder builder, final byte[] value, final SendCallback callback) {
        write(builder, new Payload(builder.getTaskName(), value, callback));
    }

    /**
     * Write bytes to this characteristic, encrypting and splitting it into chunks if necessary. Uses
     * the provided if we need to schedule something, otherwise it will be queued as other commands.
     */
    public void write(final TransactionBuilder builder, final byte[] value) {
        write(builder, value, null);
    }

    private void write(final TransactionBuilder builder, final Payload payload) {
        payloadQueue.add(payload);
        sendNext(builder);
    }

    private void sendChunk(final TransactionBuilder builder, final int index, final int chunkPayloadSize) {
        final byte[] payload = currentPayload.getBytesToSend();
        final int startIndex = index * chunkPayloadSize;
        final int endIndex = Math.min((index + 1) * chunkPayloadSize, payload.length);
        LOG.debug("Sending chunk {} from {} to {} for {}", index, startIndex, endIndex, currentPayload.getTaskName());
        final byte[] chunkToSend = new byte[2 + endIndex - startIndex];
        BLETypeConversions.writeUint16(chunkToSend, 0, index + 1);
        System.arraycopy(payload, startIndex, chunkToSend, 2, endIndex - startIndex);
        builder.write(bluetoothGattCharacteristic, chunkToSend);
    }

    public void dispose() {
        cancelTimeoutTask();
    }

    private void requestMissingChunks() {
        if (!(numChunks > 0)) {
            LOG.warn("Timeout task ran but not expecting any chunks");
            return;
        }

        LOG.debug("Timeout reached while waiting for all chunks from device");
        final List<Integer> missingChunks = new ArrayList<>();
        for (int i = 0; i < numChunks; i++) {
            if (!this.receivedChunks.containsKey(i + 1)) {
                missingChunks.add(i + 1);
            }
        }

        // prevent going over maximum message length
        int reqChunkCount = Math.min(missingChunks.size(), (maxWriteSize - 4) / 2);
        if (reqChunkCount < missingChunks.size()) {
            LOG.debug("Missing {} chunk(s), only requesting first {}: {}", missingChunks.size(), reqChunkCount, missingChunks);
        } else {
            LOG.debug("Missing {} chunk(s): {}", missingChunks.size(), missingChunks);
        }

        final ByteBuffer bb = ByteBuffer.allocate(4 + reqChunkCount * 2).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) 0); // chunk ID
        bb.put((byte) 1); // type CHUNKED_ACK
        bb.put((byte) 5); // indicate partially received transmission, followed by missing chunks
        for (int i = 0; i < reqChunkCount; i++) {
            bb.putShort(missingChunks.get(i).shortValue());
        }
        final TransactionBuilder tb = mSupport.createTransactionBuilder(String.format("send nack with missing chunks %s", missingChunks));
        tb.write(bluetoothGattCharacteristic, bb.array());
        tb.queue(mSupport.getQueue());
    }

    private void cancelTimeoutTask() {
        this.timeoutHandler.removeCallbacksAndMessages(null);
    }

    private void rescheduleTimeoutTask() {
        cancelTimeoutTask();
        this.timeoutHandler.postDelayed(this::requestMissingChunks, TIMEOUT_TASK_DELAY);
    }

    private byte[] reconstructPayloadFromChunks() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            for (int i = 0; i < this.numChunks; i++) {
                if (!this.receivedChunks.containsKey(i + 1) || this.receivedChunks.get(i + 1) == null) {
                    LOG.error("Missing chunk {}", i + 1);
                    return new byte[0];
                }

                out.write(this.receivedChunks.get(i + 1));
            }
        } catch (final IOException ex) {
            LOG.error("Failed to reconstruct payload", ex);
            return new byte[0];
        }

        return out.toByteArray();
    }

    public void onCharacteristicChanged(final byte[] value) {
        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        final int chunk = buf.getShort();
        if (chunk != 0) {
            // Chunked packet
            LOG.debug("Got chunk {} of {}", chunk, numChunks);

            if (chunk > numChunks) {
                LOG.warn("Ignoring chunk {} exceeding upper bound {}", chunk, numChunks);
                return;
            }

            if (this.receivedChunks.containsKey(chunk)) {
                LOG.warn("Already received chunk {}", chunk);
            }

            final byte[] chunkBytes = new byte[buf.limit() - buf.position()];
            buf.get(chunkBytes);
            this.receivedChunks.put(chunk, chunkBytes);
            rescheduleTimeoutTask();

            if (this.receivedChunks.keySet().size() == numChunks) {
                cancelTimeoutTask();
                sendChunkEndAck();
                final byte[] payload = reconstructPayloadFromChunks();

                if (payload.length == 0) {
                    LOG.warn("Payload reconstructed from chunks was empty");
                } else if (channelHandler != null) {
                    if (isEncrypted) {
                        // chunks are always encrypted if an auth service is available
                        channelHandler.handle(authService.decrypt(payload));
                    } else {
                        channelHandler.handle(payload);
                    }
                } else {
                    LOG.warn("Channel handler for char {} is null!", characteristicUUID);
                }

                this.numChunks = 0;
                this.receivedChunks.clear();
            }
        } else {
            // Not a chunk / single-packet
            final byte type = buf.get();

            switch (type) {
                case 0:
                    // Chunked start request
                    // TODO verify previous transfer completed
                    final byte messageEncrypted = buf.get();
                    byte expectedResult = (byte) (isEncrypted ? 1 : 0);
                    if (messageEncrypted != expectedResult) {
                        LOG.warn("Chunked start request: expected {}, got {}", expectedResult, messageEncrypted);
                        return;
                    }
                    this.numChunks = buf.getShort();
                    this.receivedChunks.clear();
                    LOG.debug("Got chunked start request for {} chunks", this.numChunks);
                    sendChunkStartAck();
                    return;
                case 1:
                    // Chunked ack
                    final byte subtype = buf.get();

                    final byte[] remaining = new byte[buf.remaining()];
                    if (buf.hasRemaining()) {
                        buf.get(remaining);
                        LOG.debug("Operation CHUNK_ACK of type {} has additional payload: {}",
                                subtype, GB.hexdump(remaining));
                    }

                    switch (subtype) {
                        case 0: {
                            LOG.debug("Got chunked ack end");
                            if (currentPayload != null && currentPayload.getCallback() != null) {
                                currentPayload.getCallback().onSend();
                            }
                            currentPayload = null;
                            sendingChunked = false;
                            sendNext(null);
                            return;
                        }
                        case 1: {
                            LOG.debug("Got chunked ack start");
                            final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunks for " + currentPayload.getTaskName());
                            final byte[] payload = currentPayload.getBytesToSend();
                            final int chunkPayloadSize = maxWriteSizeForCurrentMessage - 2;

                            for (int i = 0; i * chunkPayloadSize < payload.length; i++) {
                                sendChunk(builder, i, chunkPayloadSize);
                            }

                            builder.queue(mSupport.getQueue());
                            return;
                        }
                        case 2: {
                            LOG.warn("Got chunked nack for {}", currentPayload.getTaskName());
                            if (currentPayload != null && currentPayload.getCallback() != null) {
                                currentPayload.getCallback().onNack();
                            }
                            currentPayload = null;
                            sendingChunked = false;
                            sendNext(null);
                            return;
                        }
                        case 5: {
                            short[] invalidChunks = new short[remaining.length / 2];
                            if (remaining.length > 0) {
                                ByteBuffer remainingBuffer = ByteBuffer.wrap(remaining).order(ByteOrder.LITTLE_ENDIAN);
                                for (int i = 0; i < remaining.length / 2; i++) {
                                    invalidChunks[i] = remainingBuffer.getShort();
                                }

                                LOG.info("Got chunk request, requested chunks: {}", Arrays.toString(invalidChunks));
                                final TransactionBuilder builder = mSupport.createTransactionBuilder("resend chunks for " + currentPayload.getTaskName());

                                for (short chunkIndex : invalidChunks) {
                                    // chunk indices start at 1
                                    sendChunk(builder, chunkIndex - 1, maxWriteSizeForCurrentMessage - 2);
                                }
                            } else {
                                LOG.warn("Got chunk request, no chunk indices requested");

                                if (maxWriteSize != maxWriteSizeForCurrentMessage) {
                                    LOG.info("MTU changed while sending message, prepending message to queue and resending");
                                    ((LinkedList<Payload>) payloadQueue).addFirst(currentPayload);
                                    currentPayload = null;
                                    sendingChunked = false;
                                    sendNext(null);
                                    return;
                                }
                            }
                        }
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

                    if (channelHandler != null)
                        channelHandler.handle(plainValue);
                    else
                        LOG.warn("Channel handler for char {} is null!", characteristicUUID);

                    return;
                case 3:
                    // ack
                    final byte result = buf.get();

                    if (result == 0) {
                        LOG.debug("Got ack for {}", currentPayload.getTaskName());

                        if (currentPayload != null && currentPayload.getCallback() != null) {
                            currentPayload.getCallback().onSend();
                        }
                    } else {
                        LOG.warn("Got single cmd NACK ({}) for {}", result, currentPayload.getTaskName());

                        if (currentPayload != null && currentPayload.getCallback() != null) {
                            currentPayload.getCallback().onNack();
                        }
                    }

                    currentPayload = null;
                    waitingAck = false;
                    sendNext(null);
                    return;
            }

            LOG.warn("Unhandled command type {}", type);
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

        // before checking whether message should be chunked, read the maximum message size for this transaction
        maxWriteSizeForCurrentMessage = maxWriteSize;

        if (shouldWriteChunked(currentPayload.getBytesToSend())) {
            if (encrypt && incrementNonce) {
                // Prepend encrypted index for the nonce
                currentPayload.setBytesToSend(
                        ByteBuffer.allocate(2 + currentPayload.getBytesToSend().length).order(ByteOrder.LITTLE_ENDIAN)
                                .putShort((short) encryptedIndex++)
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
            buf.putShort((short) Math.ceil(currentPayload.getBytesToSend().length / (float) (maxWriteSizeForCurrentMessage - 2)));

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
                    buf.putShort((short) encryptedIndex++);
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
        return payload.length + 6 > maxWriteSizeForCurrentMessage;
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

    public void setMtu(final int newMtu) {
        // subtract ATT packet header size
        maxWriteSize = newMtu - 3;
    }

    private static class Payload {
        private final String taskName;
        private final byte[] bytes;

        // Bytes that will actually be sent (might be encrypted)
        private byte[] bytesToSend;
        private final SendCallback callback;

        public Payload(final String taskName, final byte[] bytes, final SendCallback callback) {
            this.taskName = taskName;
            this.bytes = bytes;
            this.callback = callback;
        }

        public Payload(final String taskName, final byte[] bytes) {
            this(taskName, bytes, null);
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
        public SendCallback getCallback() { return this.callback; }
    }

    public interface SendCallback {
        void onSend();
        void onNack();
    }
}

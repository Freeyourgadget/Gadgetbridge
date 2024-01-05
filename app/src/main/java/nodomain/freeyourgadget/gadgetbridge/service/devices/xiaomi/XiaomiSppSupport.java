/*  Copyright (C) 2023 José Rebelo, Yoran Vulker

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

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacket.CHANNEL_FITNESS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacket.CHANNEL_MASS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacket.CHANNEL_PROTO_RX;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacket.DATA_TYPE_ENCRYPTED;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacket.PACKET_PREAMBLE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.AbstractBTBRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.PlainAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetProgressAction;

public class XiaomiSppSupport extends XiaomiConnectionSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSppSupport.class);

    AbstractBTBRDeviceSupport commsSupport = new AbstractBTBRDeviceSupport(LOG) {
        @Override
        public boolean useAutoConnect() {
            return mXiaomiSupport.useAutoConnect();
        }

        @Override
        public void onSocketRead(byte[] data) {
            XiaomiSppSupport.this.onSocketRead(data);
        }

        @Override
        public boolean getAutoReconnect() {
            return mXiaomiSupport.getAutoReconnect();
        }

        @Override
        protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
            // FIXME unsetDynamicState unsets the fw version, which causes problems..
            if (getDevice().getFirmwareVersion() == null && mXiaomiSupport.getCachedFirmwareVersion() != null) {
                getDevice().setFirmwareVersion(mXiaomiSupport.getCachedFirmwareVersion());
            }

            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
            mXiaomiSupport.getAuthService().startEncryptedHandshake(XiaomiSppSupport.this, builder);

            return builder;
        }

        @Override
        protected UUID getSupportedService() {
            return XiaomiUuids.UUID_SERVICE_SERIAL_PORT_PROFILE;
        }
    };

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final AtomicInteger frameCounter = new AtomicInteger(0);
    private final AtomicInteger encryptionCounter = new AtomicInteger(0);
    private final XiaomiSupport mXiaomiSupport;
    private final Map<Integer, XiaomiChannelHandler> mChannelHandlers = new HashMap<>();

    public XiaomiSppSupport(final XiaomiSupport xiaomiSupport) {
        this.mXiaomiSupport = xiaomiSupport;

        mChannelHandlers.put(CHANNEL_PROTO_RX, this.mXiaomiSupport::handleCommandBytes);
        mChannelHandlers.put(CHANNEL_FITNESS, this.mXiaomiSupport.getHealthService().getActivityFetcher()::addChunk);
    }

    @Override
    public boolean connect() {
        return commsSupport.connect();
    }

    @Override
    public void onAuthSuccess() {
        // Do nothing.
    }

    @Override
    public void onUploadProgress(final int textRsrc, final int progressPercent, final boolean ongoing) {
        try {
            final TransactionBuilder builder = commsSupport.createTransactionBuilder("send data upload progress");
            builder.add(new SetProgressAction(
                    commsSupport.getContext().getString(textRsrc),
                    ongoing,
                    progressPercent,
                    commsSupport.getContext()
            ));
            builder.queue(commsSupport.getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }

    @Override
    public void runOnQueue(String taskName, Runnable runnable) {
        if (commsSupport == null) {
            LOG.error("commsSupport is null, unable to queue task");
            return;
	}

        final TransactionBuilder b = commsSupport.createTransactionBuilder("run task " + taskName + " on queue");
        b.add(new PlainAction() {
            @Override
            public boolean run(BluetoothSocket socket) {
                runnable.run();
                return true;
            }
        });
        b.queue(commsSupport.getQueue());
    }

    @Override
    public void setContext(GBDevice device, BluetoothAdapter adapter, Context context) {
        this.commsSupport.setContext(device, adapter, context);
    }

    @Override
    public void disconnect() {
        this.commsSupport.disconnect();
    }

    private int findNextPossiblePreamble(final byte[] haystack) {
        for (int i = 1; i + 2 < haystack.length; i++) {
            // check if first byte matches
            if (haystack[i] == PACKET_PREAMBLE[0]) {
                return i;
            }
        }

        // did not find preamble
        return -1;
    }

    private void processBuffer() {
        // wait until at least an empty packet is in the buffer
        while (buffer.size() >= 11) {
            // start preamble compare
            byte[] bufferState = buffer.toByteArray();
            ByteBuffer headerBuffer = ByteBuffer.wrap(bufferState, 0, 7).order(ByteOrder.LITTLE_ENDIAN);
            byte[] preamble = new byte[PACKET_PREAMBLE.length];
            headerBuffer.get(preamble);

            if (!Arrays.equals(PACKET_PREAMBLE, preamble)) {
                int preambleOffset = findNextPossiblePreamble(bufferState);

                if (preambleOffset == -1) {
                    LOG.debug("Buffer did not contain a valid (start of) preamble, resetting");
                    buffer.reset();
                } else {
                    LOG.debug("Found possible preamble at offset {}, dumping preceeding bytes", preambleOffset);
                    byte[] remaining = new byte[bufferState.length - preambleOffset];
                    System.arraycopy(bufferState, preambleOffset, remaining, 0, remaining.length);
                    buffer.reset();
                    try {
                        buffer.write(remaining);
                    } catch (IOException ex) {
                        LOG.error("Failed to write bytes from found preamble offset back to buffer: ", ex);
                    }
                }

                // continue processing at beginning of new buffer
                continue;
            }

            headerBuffer.getShort(); // skip flags and channel ID
            int payloadSize = headerBuffer.getShort() & 0xffff;
            int packetSize = payloadSize + 8; // payload size includes payload header

            if (bufferState.length < packetSize) {
                LOG.debug("Packet buffer not yet satisfied: buffer size {} < expected packet size {}", bufferState.length, packetSize);
                return;
            }

            LOG.debug("Full packet in buffer (buffer size: {}, packet size: {})", bufferState.length, packetSize);
            XiaomiSppPacket receivedPacket = XiaomiSppPacket.decode(bufferState); // remaining bytes unaffected

            onPacketReceived(receivedPacket);

            // extract remaining bytes from buffer
            byte[] remaining = new byte[bufferState.length - packetSize];
            System.arraycopy(bufferState, packetSize, remaining, 0, remaining.length);

            buffer.reset();

            try {
                buffer.write(remaining);
            } catch (IOException ex) {
                LOG.error("Failed to write remaining packet bytes back to buffer: ", ex);
            }
        }
    }

    @Override
    public void dispose() {
        commsSupport.dispose();
    }

    public void onSocketRead(byte[] data) {
        try {
            buffer.write(data);
        } catch (IOException ex) {
            LOG.error("Exception while writing buffer: ", ex);
        }

        processBuffer();
    }

    private void onPacketReceived(final XiaomiSppPacket packet) {
        if (packet == null) {
            // likely failed to parse the packet
            LOG.warn("Received null packet, did we fail to decode?");
            return;
        }

        LOG.debug("Packet received: {}", packet);
        // TODO send response if needsResponse is set
        byte[] payload = packet.getPayload();

        if (packet.getDataType() == 1) {
            payload = mXiaomiSupport.getAuthService().decrypt(payload);
        }

        int channel = packet.getChannel();
        if (mChannelHandlers.containsKey(channel)) {
            XiaomiChannelHandler handler = mChannelHandlers.get(channel);

            if (handler != null)
                handler.handle(payload);
        }

        LOG.warn("Unhandled SppPacket on channel {}", packet.getChannel());
    }

    @Override
    public void sendCommand(String taskName, XiaomiProto.Command command) {
        XiaomiSppPacket packet = XiaomiSppPacket.fromXiaomiCommand(command, frameCounter.getAndIncrement(), false);
        LOG.debug("sending packet: {}", packet);
        TransactionBuilder builder = this.commsSupport.createTransactionBuilder("send " + taskName);
        builder.write(packet.encode(mXiaomiSupport.getAuthService(), encryptionCounter));
        builder.queue(this.commsSupport.getQueue());
    }

    public void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command) {
        XiaomiSppPacket packet = XiaomiSppPacket.fromXiaomiCommand(command, frameCounter.getAndIncrement(), false);
        LOG.debug("sending packet: {}", packet);

        builder.write(packet.encode(mXiaomiSupport.getAuthService(), encryptionCounter));
        // do not queue here, that's the job of the caller
    }

    public void sendDataChunk(final String taskName, final byte[] chunk, @Nullable final XiaomiCharacteristic.SendCallback callback) {
        XiaomiSppPacket packet = XiaomiSppPacket.newBuilder()
                .channel(CHANNEL_MASS)
                .needsResponse(false)
                .flag(true)
                .opCode(2)
                .frameSerial(frameCounter.getAndIncrement())
                .dataType(DATA_TYPE_ENCRYPTED)
                .payload(chunk)
                .build();
        LOG.debug("sending data packet: {}", packet);
        TransactionBuilder b = this.commsSupport.createTransactionBuilder("send " + taskName);
        b.write(packet.encode(mXiaomiSupport.getAuthService(), encryptionCounter));
        b.queue(commsSupport.getQueue());

        if (callback != null) {
            // callback puts a SetProgressAction onto the queue
            callback.onSend();
        }
    }
}

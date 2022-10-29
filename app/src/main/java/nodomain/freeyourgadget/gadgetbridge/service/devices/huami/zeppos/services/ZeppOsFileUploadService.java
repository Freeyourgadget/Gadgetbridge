/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class ZeppOsFileUploadService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFileUploadService.class);

    private static final short ENDPOINT = 0x000d;

    private static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    private static final byte CMD_UPLOAD_REQUEST = 0x03;
    private static final byte CMD_UPLOAD_RESPONSE = 0x04;
    private static final byte CMD_DATA_SEND = 0x10;
    private static final byte CMD_DATA_ACK = 0x11;

    private static final byte FLAG_FIRST_CHUNK = 0x01;
    private static final byte FLAG_LAST_CHUNK = 0x02;

    private final Map<Byte, FileSendRequest> mSessionRequests = new HashMap<>();

    private int mChunkSize = -1;

    public ZeppOsFileUploadService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        byte session;
        byte status;

        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                final int version = payload[1] & 0xff;
                if (version != 1 && version != 2) {
                    LOG.error("Unsupported file upload service version: {}", version);
                    return;
                }
                mChunkSize = BLETypeConversions.toUint16(payload, 2);
                LOG.info("Got file upload service: version={}, chunkSize={}", version, mChunkSize);
                return;
            case CMD_UPLOAD_RESPONSE:
                session = payload[1];
                status = payload[2];
                final int existingProgress = BLETypeConversions.toUint32(payload, 3);
                LOG.info("Band acknowledged file upload request: session={}, status={}, existingProgress={}", session, status, existingProgress);
                if (status != 0) {
                    LOG.error("Unexpected status from band for session {}, aborting", session);
                    onFinish(session, false);
                    return;
                }
                if (existingProgress != 0) {
                    LOG.info("Updating existing progress for session {} to {}", session, existingProgress);
                    final FileSendRequest request = mSessionRequests.get(session);
                    if (request == null) {
                        LOG.error("No request found for session {}", session);
                        return;
                    }
                    request.setProgress(existingProgress);
                }
                sendNextQueuedData(session);
                return;
            case CMD_DATA_ACK:
                session = payload[1];
                status = payload[2];
                LOG.info("Band acknowledged file upload data: session={}, status={}", session, status);
                if (status != 0) {
                    LOG.error("Unexpected status from band, aborting session {}", session);
                    onFinish(session, false);
                    return;
                }
                sendNextQueuedData(session);
                return;
            default:
                LOG.warn("Unexpected file upload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public void requestCapability(final TransactionBuilder builder) {
        write(builder, new byte[]{CMD_CAPABILITIES_REQUEST});
    }

    public void sendFile(final String url, final String filename, final byte[] bytes, final Callback callback) {
        if (mChunkSize < 0) {
            LOG.error("Service not initialized, refusing to send {}", url);
            callback.onFinish(false);
            return;
        }

        LOG.info("Sending {} bytes to {}", bytes.length, url);

        final FileSendRequest request = new FileSendRequest(url, filename, bytes, callback);

        final byte session = (byte) mSessionRequests.size();

        final ByteBuffer buf = ByteBuffer.allocate(2 + url.length() + 1 + filename.length() + 1 + 4 + 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_UPLOAD_REQUEST);
        buf.put(session);
        buf.put(url.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.put(filename.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.putInt(bytes.length);
        buf.putInt(CheckSums.getCRC32(bytes));

        write("send file upload request", buf.array());

        mSessionRequests.put(session, request);
    }

    private void sendNextQueuedData(final byte session) {
        final FileSendRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No request found for session {}", session);
            return;
        }

        if (request.getProgress() >= request.getSize()) {
            LOG.info("Sending {} finished", request.getUrl());
            onFinish(session, true);
            return;
        }

        LOG.debug("Sending file data for session={}, progress={}, index={}", session, request.getProgress(), request.getIndex());

        final ByteBuffer buf = ByteBuffer.allocate(10 + mChunkSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_DATA_SEND);

        byte flags = 0;
        if (request.getProgress() == 0) {
            flags |= FLAG_FIRST_CHUNK;
        }
        if (request.getProgress() + mChunkSize >= request.getSize()) {
            flags |= FLAG_LAST_CHUNK;
        }

        buf.put(flags);
        buf.put(session);
        buf.put(request.getIndex());
        if ((flags & FLAG_FIRST_CHUNK) > 0) {
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
        }

        final byte[] payload = ArrayUtils.subarray(
                request.getBytes(),
                request.getProgress(),
                request.getProgress() + mChunkSize
        );

        buf.putShort((short) payload.length);
        buf.put(payload);

        request.setProgress(request.getProgress() + payload.length);
        request.setIndex((byte) (request.getIndex() + 1));
        request.getCallback().onProgress(request.getProgress());

        write("send file data", buf.array());
    }

    private void onFinish(final byte session, final boolean success) {
        final FileSendRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No request found for session {}", session);
            return;
        }

        mSessionRequests.remove(session);

        request.getCallback().onFinish(success);
    }

    /**
     * Wrapper class to keep track of ongoing file send requests and their progress.
     */
    public static class FileSendRequest {
        private final String url;
        private final String filename;
        private final byte[] bytes;
        private final Callback callback;
        private int progress = 0;
        private byte index = 0;

        public FileSendRequest(final String url, final String filename, final byte[] bytes, final Callback callback) {
            this.url = url;
            this.filename = filename;
            this.bytes = bytes;
            this.callback = callback;
        }

        public String getUrl() {
            return url;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public int getSize() {
            return bytes.length;
        }

        public Callback getCallback() {
            return callback;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(final int progress) {
            this.progress = progress;
        }

        public byte getIndex() {
            return index;
        }

        public void setIndex(final byte index) {
            this.index = index;
        }
    }

    public interface Callback {
        void onFinish(boolean success);

        void onProgress(int progress);
    }
}

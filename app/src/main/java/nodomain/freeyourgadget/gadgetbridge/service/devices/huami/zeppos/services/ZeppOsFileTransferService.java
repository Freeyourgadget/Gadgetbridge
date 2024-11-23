/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsFileTransferService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFileTransferService.class);

    private static final short ENDPOINT = 0x000d;

    private static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    private static final byte CMD_TRANSFER_REQUEST = 0x03;
    private static final byte CMD_TRANSFER_RESPONSE = 0x04;
    private static final byte CMD_DATA_SEND = 0x10;
    private static final byte CMD_DATA_ACK = 0x11;
    private static final byte CMD_DATA_V3_SEND = 0x12;
    private static final byte CMD_DATA_V3_ACK = 0x13;

    private static final byte FLAG_FIRST_CHUNK = 0x01;
    private static final byte FLAG_LAST_CHUNK = 0x02;
    private static final byte FLAG_CRC = 0x04;

    private final Map<Byte, FileTransferRequest> mSessionRequests = new HashMap<>();

    private int mVersion = -1;
    private int mChunkSize = -1;
    private int mCompressedChunkSize = -1;

    public ZeppOsFileTransferService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        byte session;
        byte status;

        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                mVersion = payload[1] & 0xff;
                if (mVersion != 1 && mVersion != 2 && mVersion != 3) {
                    LOG.error("Unsupported file transfer service version: {}", mVersion);
                    return;
                }
                mChunkSize = BLETypeConversions.toUint16(payload, 2);
                if (mVersion == 3) {
                    // TODO parse the rest for v3
                    mCompressedChunkSize = BLETypeConversions.toUint32(payload, 4);
                    final TransactionBuilder builder = getSupport().createTransactionBuilder("enable file transfer v3 notifications");
                    builder.notify(getSupport().getCharacteristic(HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3), true);
                    builder.queue(getSupport().getQueue());
                }
                LOG.info(
                    "Got file transfer service: version={}, chunkSize={}, compressedChunkSize={}",
                    mVersion,
                    mChunkSize,
                    mCompressedChunkSize
                );
                return;
            case CMD_TRANSFER_REQUEST:
                handleFileTransferRequest(payload);
                return;
            case CMD_TRANSFER_RESPONSE:
                session = payload[1];
                status = payload[2];
                final int existingProgress = BLETypeConversions.toUint32(payload, 3);
                LOG.info("Band acknowledged file transfer request: session={}, status={}, existingProgress={}", session, status, existingProgress);
                if (status != 0) {
                    LOG.error("Unexpected status from band for session {}, aborting", session);
                    onUploadFinish(session, false);
                    return;
                }
                if (existingProgress != 0) {
                    LOG.info("Updating existing progress for session {} to {}", session, existingProgress);
                    final FileTransferRequest request = mSessionRequests.get(session);
                    if (request == null) {
                        LOG.error("No request found for session {}", session);
                        return;
                    }
                    request.setProgress(existingProgress);
                }
                sendNextQueuedData(session);
                return;
            case CMD_DATA_SEND:
                handleFileTransferData(payload);
                return;
            case CMD_DATA_ACK:
                session = payload[1];
                status = payload[2];
                LOG.info("Band acknowledged file transfer data: session={}, status={}", session, status);
                if (status != 0) {
                    LOG.error("Unexpected status from band, aborting session {}", session);
                    onUploadFinish(session, false);
                    return;
                }
                sendNextQueuedData(session);
                return;
            default:
                LOG.warn("Unexpected file transfer byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapability(builder);
    }

    public void requestCapability(final TransactionBuilder builder) {
        write(builder, new byte[]{CMD_CAPABILITIES_REQUEST});
    }

    private void handleFileTransferRequest(final byte[] payload) {
        // File transfer request initialized from watch
        int pos = 1;
        final byte session = payload[pos++];
        final String url = StringUtils.untilNullTerminator(payload, pos);
        if (url == null) {
            LOG.error("Unable to parse url from transfer request");
            return;
        }
        pos += url.length() + 1;
        final String filename = StringUtils.untilNullTerminator(payload, pos);
        if (filename == null) {
            LOG.error("Unable to parse filename from transfer request");
            return;
        }
        pos += filename.length() + 1;
        final int length = BLETypeConversions.toUint32(payload, pos);
        pos += 4;
        final int crc32 = BLETypeConversions.toUint32(payload, pos);
        pos += 4;

        final boolean compressed;
        if (pos < payload.length) {
            final Boolean compressedBoolean = booleanFromByte(payload[pos]);
            if (compressedBoolean == null) {
                LOG.warn("Unknown compression type {}", payload[pos]);
                return;
            }
            compressed = compressedBoolean;
        } else {
            compressed = false;
        }

        LOG.info("Got transfer request: session={}, url={}, filename={}, length={}, compressed={}", session, url, filename, length, compressed);

        final FileTransferRequest request = new FileTransferRequest(
                url,
                filename,
                new byte[length],
                compressed,
                compressed ? mCompressedChunkSize : mChunkSize,
                getSupport()
        );
        request.setCrc32(crc32);

        if (mVersion < 3) {
            final ByteBuffer buf = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put(CMD_TRANSFER_RESPONSE);
            buf.put(session);
            buf.put((byte) 0x00);
            buf.putInt(0);

            write("send file transfer response", buf.array());
        } else {
            // FIXME: Receive files on v3
            LOG.error("Receiving files on V3 is not implemented");
            return;
        }

        mSessionRequests.put(session, request);
    }

    private void handleFileTransferData(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // Discard first byte
        final byte flags = buf.get();
        final boolean firstPacket = (flags == 1);
        final boolean lastPacket = (flags == 2);
        final byte session = buf.get();
        final byte index = buf.get();

        if ((flags & 0x01) != 0) {
            buf.getInt(); // ?
        }

        final short size = buf.getShort();

        final FileTransferRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No request found for session {}", session);
            return;
        }

        if (index != request.index) {
            LOG.warn("Unexpected index {}, expected {}", index, request.index);
            return;
        }

        if (firstPacket && request.getProgress() != 0) {
            LOG.warn("Got first packet, but progress is {}", request.getProgress());
            return;
        }

        buf.get(request.getBytes(), request.getProgress(), size);
        request.setIndex((byte) (index + 1));
        request.setProgress(request.getProgress() + size);

        LOG.debug("Got data for session={}, progress={}/{}", session, request.getProgress(), request.getSize());

        write("ack file data", new byte[]{CMD_DATA_ACK, session, 0x00});

        if (lastPacket) {
            mSessionRequests.remove(session);

            final byte[] data;
            if (request.isCompressed()) {
                data = decompress(request.getBytes());
                if (data == null) {
                    LOG.error("Failed to decompress bytes for session={}", session);
                    return;
                }
            } else {
                data = request.getBytes();
            }

            final int checksum = CheckSums.getCRC32(data);
            if (checksum != request.getCrc32()) {
                LOG.warn("Checksum mismatch: expected {}, got {}", request.getCrc32(), checksum);
                return;
            }

            request.getCallback().onFileDownloadFinish(request.getUrl(), request.getFilename(), data);
        }
    }

    public static byte[] compress(final byte[] data) {
        final Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        final byte[] buf = new byte[8096];
        int read;
        while ((read = deflater.deflate(buf)) > 0) {
            baos.write(buf, 0, read);
        }

        return baos.toByteArray();
    }

    public static byte[] decompress(final byte[] data) {
        final Inflater inflater = new Inflater();
        final byte[] output = new byte[data.length];
        inflater.setInput(data);
        try {
            inflater.inflate(output);
        } catch (final DataFormatException e) {
            LOG.error("Failed to decompress data", e);
            return null;
        } finally {
            inflater.end();
        }

        return output;
    }

    public void sendFile(final String url, final String filename, final byte[] bytes, final boolean compress, final Callback callback) {
        if (mChunkSize < 0) {
            LOG.error("Service not initialized, refusing to send {}", url);
            callback.onFileUploadFinish(false);
            return;
        }

        LOG.info("Sending {} bytes to {} in {}", bytes.length, filename, url);

        final FileTransferRequest request = new FileTransferRequest(
                url,
                filename,
                bytes,
                compress && mCompressedChunkSize > 0,
                compress && mCompressedChunkSize > 0 ? mCompressedChunkSize : mChunkSize,
                callback
        );

        if (mVersion == 3 && !mSessionRequests.isEmpty()) {
            // FIXME non-zero session on v3
            LOG.error("File transfer v3 only supports single session, not sending file");
            callback.onFileUploadFinish(false);
            return;
        }

        byte session = (byte) mSessionRequests.size();
        while (mSessionRequests.containsKey(session)) {
            session++;
        }

        int payloadSize = 2 + url.length() + 1 + filename.length() + 1 + 4 + 4;
        if (mVersion == 3) {
            payloadSize += 2;
            if (compress) {
                payloadSize += 4;
            }
        }

        final ByteBuffer buf = ByteBuffer.allocate(payloadSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_TRANSFER_REQUEST);
        buf.put(session);
        buf.put(url.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.put(filename.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.putInt(bytes.length);
        buf.putInt(request.getCrc32());
        if (mVersion == 3) {
            buf.put((byte) (compress ? 1 : 0));
            if (compress) {
                buf.putInt(mCompressedChunkSize);
            }
            buf.put((byte) 0);
        }

        write("send file upload request", buf.array());

        mSessionRequests.put(session, request);
    }

    private void sendNextQueuedData(final byte session) {
        final FileTransferRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No request found for session {}", session);
            return;
        }

        if (request.getProgress() >= request.getSize()) {
            LOG.info("Finished sending {}", request.getUrl());
            onUploadFinish(session, true);
            return;
        }

        LOG.debug("Sending file data for session={}, progress={}, index={}", session, request.getProgress(), request.getIndex());

        if (mVersion < 3) {
            writeChunkV1(request, session);
        } else {
            if (session != 0) {
                // FIXME non-zero session on v3
                LOG.error("Sending non-zero session on v3 is not supported, got session={}", session);
                mSessionRequests.remove(session);
                return;
            }
            writeChunkV3(request);
        }
    }

    private void writeChunkV1(final FileTransferRequest request, final byte session) {
        final ByteBuffer buf = ByteBuffer.allocate(10 + request.getChunkSize());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_DATA_SEND);

        byte flags = 0;
        if (request.getProgress() == 0) {
            flags |= FLAG_FIRST_CHUNK;
        }
        if (request.getProgress() + request.getChunkSize() >= request.getSize()) {
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
                request.getProgress() + request.getChunkSize()
        );

        buf.putShort((short) payload.length);
        buf.put(payload);

        request.setProgress(request.getProgress() + payload.length);
        request.setIndex((byte) (request.getIndex() + 1));
        request.getCallback().onFileUploadProgress(request.getProgress());

        write("send file data", buf.array());
    }

    private void writeChunkV3(final FileTransferRequest request) {
        final byte[] chunk = ArrayUtils.subarray(
                request.getBytes(),
                request.getProgress(),
                request.getProgress() + request.getChunkSize()
        );

        byte flags = 0;
        if (request.getProgress() == 0) {
            flags |= FLAG_FIRST_CHUNK;
        }
        if (request.getProgress() + request.getChunkSize() >= request.getSize()) {
            flags |= FLAG_LAST_CHUNK;
        }

        final int partSize = getSupport().getMTU() - 3;

        final ByteBuffer buf = ByteBuffer.allocate(chunk.length + 5);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_DATA_V3_SEND);
        buf.put(flags);
        buf.put(request.getIndex());
        buf.putShort((short) chunk.length);
        buf.put(chunk);

        final byte[] payload = buf.array();

        final TransactionBuilder builder = getSupport().createTransactionBuilder("send chunk v3");
        for (int i = 0; i < payload.length; i += partSize) {
            final byte[] part = ArrayUtils.subarray(payload, i, i + partSize);
            builder.write(
                    getSupport().getCharacteristic(HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3),
                    part
            );
        }
        builder.queue(getSupport().getQueue());

        request.setProgress(request.getProgress() + chunk.length);
        request.setIndex((byte) (request.getIndex() + 1));
        request.getCallback().onFileUploadProgress(request.getProgress());
    }

    private void onUploadFinish(final byte session, final boolean success) {
        final FileTransferRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No request found for session {}", session);
            return;
        }

        mSessionRequests.remove(session);

        request.getCallback().onFileUploadFinish(success);
    }

    public void onCharacteristicChanged(final byte[] value) {
        if (value[0] != CMD_DATA_V3_ACK) {
            LOG.error("Got non-ack on file transfer characteristic");
            return;
        }

        final byte session = (byte) 0; // FIXME non-zero session on v3
        final byte status = value[1];
        final byte chunkIndex = value[2];
        final byte unk1 = value[3]; // 1/2?

        LOG.info(
                "Band acknowledged file transfer data: session={}, status={}, chunkIndex={}, unk1={}",
                session,
                status,
                chunkIndex,
                unk1
        );

        final FileTransferRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No request found for v3 session {}", session);
            return;
        }

        if (status != 0) {
            LOG.error("Unexpected status from band, aborting session {}", session);
            onUploadFinish(session, false);
            return;
        }

        if (request.getIndex() - 1 != chunkIndex) {
            LOG.error("Got ack for unexpected chunk index {}, expected {}", chunkIndex, request.getIndex() - 1);
            onUploadFinish(session, false);
            return;
        }

        sendNextQueuedData(session);
    }

    /**
     * Wrapper class to keep track of ongoing file send requests and their progress.
     */
    public static class FileTransferRequest {
        private final String url;
        private final String filename;
        private final byte[] bytes;
        private final boolean compressed;
        private final int chunkSize;
        private final Callback callback;
        private int progress = 0;
        private byte index = 0;
        private int crc32;

        public FileTransferRequest(final String url, final String filename, final byte[] bytes, boolean compressed, int chunkSize, final Callback callback) {
            this.url = url;
            this.filename = filename;
            this.bytes = compressed ? compress(bytes) : bytes;
            this.compressed = compressed;
            this.chunkSize = chunkSize;
            this.callback = callback;
            this.crc32 = CheckSums.getCRC32(bytes);
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

        public boolean isCompressed() {
            return compressed;
        }

        public int getChunkSize() {
            return chunkSize;
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

        public int getCrc32() {
            return crc32;
        }

        public void setCrc32(final int crc32) {
            this.crc32 = crc32;
        }
    }

    public interface Callback {
        void onFileUploadFinish(boolean success);

        void onFileUploadProgress(int progress);

        void onFileDownloadFinish(String url, String filename, byte[] data);
    }
}

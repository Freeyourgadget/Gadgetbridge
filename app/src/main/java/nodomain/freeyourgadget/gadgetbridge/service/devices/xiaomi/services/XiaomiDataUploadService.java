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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import androidx.annotation.Nullable;

import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class XiaomiDataUploadService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiDataUploadService.class);

    public static final int COMMAND_TYPE = 22;

    public static final int CMD_UPLOAD_START = 0;

    public static final byte TYPE_WATCHFACE = 16;
    public static final byte TYPE_FIRMWARE = 32;
    public static final byte TYPE_NOTIFICATION_ICON = 50;

    private XiaomiCharacteristic characteristic;
    private Callback callback;

    private byte currentType;
    private byte[] currentBytes;

    private int chunkSize;

    public XiaomiDataUploadService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_UPLOAD_START:
                final XiaomiProto.DataUploadAck dataUploadAck = cmd.getDataUpload().getDataUploadAck();
                LOG.debug("Got upload start, unknown2={}, resumePosition={}", dataUploadAck.getUnknown2(), dataUploadAck.getResumePosition());

                if (dataUploadAck.getUnknown2() != 0 || dataUploadAck.getResumePosition() != 0) {
                    LOG.warn("Unexpected response");
                    this.currentType = 0;
                    this.currentBytes = null;
                    return;
                }

                if (dataUploadAck.hasChunkSize()) {
                    chunkSize = dataUploadAck.getChunkSize();
                } else {
                    chunkSize = 2048;
                }

                doUpload(currentType, currentBytes);
                return;
        }

        LOG.warn("Unknown data upload command {}", cmd.getSubtype());
    }

    public void setCallback(@Nullable final Callback callback) {
        if (callback != null && currentBytes != null) {
            LOG.warn("Already uploading {} for another callback, refusing new callback", currentType);
            return;
        }

        this.callback = callback;
    }

    public void requestUpload(final byte type, final byte[] bytes) {
        if (this.currentBytes != null) {
            LOG.warn("Already uploading {}, refusing upload of {}", currentType, type);
            return;
        }

        LOG.debug("Requesting upload for {} bytes of type {}", bytes.length, type);

        this.currentType = type;
        this.currentBytes = bytes;

        getSupport().sendCommand(
                "request upload",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_UPLOAD_START)
                        .setDataUpload(XiaomiProto.DataUpload.newBuilder().setDataUploadRequest(
                                XiaomiProto.DataUploadRequest.newBuilder()
                                        .setType(type)
                                        .setMd5Sum(ByteString.copyFrom(Objects.requireNonNull(CheckSums.md5(bytes))))
                                        .setSize(bytes.length)
                        ))
                        .build()
        );
    }

    private void doUpload(final short type, final byte[] bytes) {
        LOG.debug("Doing upload for {} bytes of type {}", bytes.length, type);

        // type + md5 + size + bytes + crc32
        final ByteBuffer buf1 = ByteBuffer.allocate(2 + 16 + 4 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        final byte[] md5 = CheckSums.md5(bytes);
        if (md5 == null) {
            onUploadFinish(false);
            return;
        }

        buf1.put((byte) 0);
        buf1.put((byte) type);
        buf1.put(md5);
        buf1.putInt(bytes.length);
        buf1.put(bytes);

        final ByteBuffer buf2 = ByteBuffer.allocate(buf1.capacity() + 4).order(ByteOrder.LITTLE_ENDIAN);
        buf2.put(buf1.array());
        buf2.putInt(CheckSums.getCRC32(buf1.array()));

        final byte[] payload = buf2.array();
        final int partSize = chunkSize - 4; // 2 + 2 at beginning of each for total and progress
        final int totalParts = (int) Math.ceil(payload.length / (float) partSize);

        characteristic.setCallback(remainingParts -> {
            final int totalBytes = totalParts * 4 + payload.length;
            int progressBytes = totalParts * 4 + payload.length;
            if (remainingParts > 1) {
                progressBytes -= (remainingParts - 1) * partSize;
            }
            if (remainingParts > 0) {
                progressBytes -= (payload.length % partSize);
            }

            final int progressPercent = Math.round((100.0f * progressBytes) / totalBytes);

            LOG.debug("Data upload progress: {} parts remaining ({}%)", remainingParts, progressPercent);

            if (remainingParts > 0) {
                if (callback != null) {
                    callback.onUploadProgress(progressPercent);
                }
            } else {
                onUploadFinish(true);
            }
        });

        for (int i = 0; i * partSize < payload.length; i++) {
            final int startIndex = i * partSize;
            final int endIndex = Math.min((i + 1) * partSize, payload.length);
            LOG.debug("Uploading part {} of {}, from {} to {}", (i + 1), totalParts, startIndex, endIndex);
            final byte[] chunkToSend = new byte[4 + endIndex - startIndex];
            BLETypeConversions.writeUint16(chunkToSend, 0, totalParts);
            BLETypeConversions.writeUint16(chunkToSend, 2, i + 1);
            System.arraycopy(payload, startIndex, chunkToSend, 4, endIndex - startIndex);
            characteristic.write("upload part " + (i + 1) + " of " + totalParts, chunkToSend);
        }
    }

    public void setDataUploadCharacteristic(final XiaomiCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    private void onUploadFinish(final boolean success) {
        this.currentType = 0;
        this.currentBytes = null;

        if (callback != null) {
            callback.onUploadFinish(success);
        }

        characteristic.setCallback(null);
    }

    public interface Callback {
        void onUploadFinish(boolean success);

        void onUploadProgress(int progress);
    }
}

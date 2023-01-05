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

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;

public class ZeppOsAgpsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAgpsService.class);

    private static final short ENDPOINT = 0x0042;

    private static final byte CMD_UPDATE_START_UPLOAD_REQUEST = 0x03;
    private static final byte CMD_UPDATE_START_UPLOAD_RESPONSE = 0x04;
    private static final byte CMD_UPDATE_START_REQUEST = 0x05;
    private static final byte CMD_UPDATE_PROGRESS_RESPONSE = 0x06;
    private static final byte CMD_UPDATE_FINISH_RESPONSE = 0x07;

    private Callback mCallback = null;

    public ZeppOsAgpsService(final Huami2021Support support) {
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
        switch (payload[0]) {
            case CMD_UPDATE_START_UPLOAD_RESPONSE:
                final byte uploadStatus = payload[1];
                LOG.info("Got agps start upload status = {}", uploadStatus);
                if (mCallback != null) {
                    mCallback.onAgpsUploadStartResponse(uploadStatus == 0x01);
                }
                return;
            case CMD_UPDATE_PROGRESS_RESPONSE:
                final int size = BLETypeConversions.toUint32(payload, 1);
                final int progress = BLETypeConversions.toUint32(payload, 5);

                LOG.info("Got agps progress = {}/{}", progress, size);
                if (mCallback != null) {
                    mCallback.onAgpsProgressResponse(size, progress);
                }
                return;
            case CMD_UPDATE_FINISH_RESPONSE:
                final byte finishStatus = payload[1];
                LOG.info("Got agps update finish status = {}", finishStatus);
                if (mCallback != null) {
                    mCallback.onAgpsUpdateFinishResponse(finishStatus == 0x01);
                }
                return;
            default:
                LOG.warn("Unexpected agps byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public void startUpload(final int size) {
        final ByteBuffer buf = ByteBuffer.allocate(5)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(CMD_UPDATE_START_UPLOAD_REQUEST)
                .putInt(size);

        write("start upload request", buf.array());
    }

    public void startUpdate() {
        write("start update request", CMD_UPDATE_START_REQUEST);
    }

    public void setCallback(@Nullable final Callback callback) {
        this.mCallback = callback;
    }

    public interface Callback {
        void onAgpsUploadStartResponse(boolean success);

        void onAgpsProgressResponse(int size, int progress);

        void onAgpsUpdateFinishResponse(boolean success);
    }
}

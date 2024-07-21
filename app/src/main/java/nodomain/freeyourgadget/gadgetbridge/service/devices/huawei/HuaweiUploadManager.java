/*  Copyright (C) 2024 Vitalii Tomin

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload.FileUploadParams;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiUploadManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiUploadManager.class);
    private final HuaweiSupportProvider support;
    byte[] fileBin;
    byte[] fileSHA256;
    byte fileType = 1; // 1 - watchface, 2 - music, 3 - png for background , 7 - app
    int fileSize = 0;
    byte fileId = 0; // get on incoming (2803)

    int currentUploadPosition = 0;
    int uploadChunkSize =0;

    String fileName = ""; //FIXME generate random name

    //ack values set from 28 4 response
    FileUploadParams fileUploadParams;


    public HuaweiUploadManager(HuaweiSupportProvider support) {
        this.support=support;
    }

    public void setBytes(byte[] uploadArray) {

        this.fileSize = uploadArray.length;
        this.fileBin = uploadArray;

        try {
            MessageDigest m = MessageDigest.getInstance("SHA256");
            m.update(fileBin, 0, fileBin.length);
            fileSHA256 =  m.digest();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Digest alghoritm not found.", e);
            return;
        }

        currentUploadPosition = 0;
        uploadChunkSize = 0;

        LOG.info("File ready for upload, SHA256: "+ GB.hexdump(fileSHA256) + " fileName: " + fileName + " filetype: ", fileType);

    }

    public int getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte getFileType() {
        return this.fileType;
    }

    public void setFileType(byte fileType) {
        this.fileType = fileType;
    }

    public byte getFileId() {
        return fileId;
    }

    public void setFileId(byte fileId) {
        this.fileId = fileId;
    }

    public byte[] getFileSHA256() {
        return fileSHA256;
    }

    public void setUploadChunkSize(int chunkSize) {
        uploadChunkSize = chunkSize;
    }

    public void setCurrentUploadPosition (int pos) {
        currentUploadPosition = pos;
    }

    public int getCurrentUploadPosition() {
        return currentUploadPosition;
    }

    public byte[] getCurrentChunk() {
        byte[] ret = new byte[uploadChunkSize];
        System.arraycopy(fileBin, currentUploadPosition, ret, 0, uploadChunkSize);
        return ret;
    }

    public void setFileUploadParams(FileUploadParams params) {
        this.fileUploadParams = params;
    }


    public short getUnitSize() {
        return fileUploadParams.unit_size;
    }

    public void setDeviceBusy() {
        final GBDevice device = support.getDevice();
        device.setBusyTask(support.getContext().getString(R.string.uploading_watchface));
        device.sendDeviceUpdateIntent(support.getContext());
    }

    public void unsetDeviceBusy() {
        final GBDevice device = support.getDevice();
        if (device != null && device.isConnected()) {
            if (device.isBusy()) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(support.getContext());
            }
            device.sendDeviceUpdateIntent(support.getContext());
        }
    }


}

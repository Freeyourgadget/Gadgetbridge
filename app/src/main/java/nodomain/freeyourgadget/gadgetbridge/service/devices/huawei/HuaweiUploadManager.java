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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload.FileUploadParams;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiUploadManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiUploadManager.class);

    public interface FileUploadCallback {
        void onUploadStart();
        void onUploadProgress(int progress);
        void onUploadComplete();
        void onError(int code);
    }

    public static class FileUploadInfo {
        private byte[] fileBin;
        private byte[] fileSHA256;
        private byte fileType = 1; // 1 - watchface, 2 - music, 3 - png for background , 7 - app
        private int fileSize = 0;
        private byte fileId = 0; // get on incoming (2803)

        private String fileName = ""; //FIXME generate random name

        private String srcPackage = null;
        private String dstPackage = null;
        private String srcFingerprint = null;
        private String dstFingerprint = null;
        private boolean isEncrypted = false;

        private int currentUploadPosition = 0;
        private int uploadChunkSize = 0;

        private FileUploadCallback fileUploadCallback = null;

        //ack values set from 28 4 response
        private FileUploadParams fileUploadParams = null;


        public FileUploadCallback getFileUploadCallback() {
            return fileUploadCallback;
        }

        public void setFileUploadCallback(FileUploadCallback fileUploadCallback) {
            this.fileUploadCallback = fileUploadCallback;
        }

        public void setFileUploadParams(FileUploadParams params) {
            this.fileUploadParams = params;
        }

        public int getUnitSize() {
            return fileUploadParams.unit_size;
        }

        public void setBytes(byte[] uploadArray) {

            this.fileSize = uploadArray.length;
            this.fileBin = uploadArray;

            try {
                MessageDigest m = MessageDigest.getInstance("SHA256");
                m.update(fileBin, 0, fileBin.length);
                fileSHA256 =  m.digest();
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Digest algorithm not found.", e);
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

        public String getSrcPackage() { return srcPackage; }

        public void setSrcPackage(String srcPackage) { this.srcPackage = srcPackage; }

        public String getDstPackage() { return dstPackage; }

        public void setDstPackage(String dstPackage) { this.dstPackage = dstPackage; }

        public String getSrcFingerprint() { return srcFingerprint; }

        public void setSrcFingerprint(String srcFingerprint) { this.srcFingerprint = srcFingerprint; }

        public String getDstFingerprint() { return dstFingerprint;}

        public void setDstFingerprint(String dstFingerprint) { this.dstFingerprint = dstFingerprint;}

        public boolean isEncrypted() { return isEncrypted; }

        public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }

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
    }

    private final HuaweiSupportProvider support;

    FileUploadInfo fileUploadInfo = null;

    public HuaweiUploadManager(HuaweiSupportProvider support) {
        this.support=support;
    }

    public FileUploadInfo getFileUploadInfo() {
        return fileUploadInfo;
    }

    public void setFileUploadInfo(FileUploadInfo fileUploadInfo) {
        this.fileUploadInfo = fileUploadInfo;
    }

    public void setDeviceBusy() {
        final GBDevice device = support.getDevice();
        if(fileUploadInfo != null && fileUploadInfo.fileType == FileUpload.Filetype.watchface) {
            device.setBusyTask(support.getContext().getString(R.string.uploading_watchface));
        } else {
            device.setBusyTask(support.getContext().getString(R.string.updating_firmware));
        }
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

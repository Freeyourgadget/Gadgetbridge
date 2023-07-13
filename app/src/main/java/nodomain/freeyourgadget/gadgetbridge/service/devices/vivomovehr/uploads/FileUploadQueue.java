/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.uploads;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.VivomoveHrCommunicator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.CreateFileRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.CreateFileResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.DownloadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FileTransferDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FileTransferDataResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.UploadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.UploadRequestResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class FileUploadQueue {
    private static final Logger LOG = LoggerFactory.getLogger(FileUploadQueue.class);
    private static final int MAX_BLOCK_SIZE = 500;
    // TODO: ?
    private static final int UPLOAD_FLAGS = 0;

    private final VivomoveHrCommunicator communicator;

    private final Queue<QueueItem> queue = new LinkedList<>();

    private QueueItem currentlyUploadingItem;
    private int currentCrc;
    private long totalRemainingBytes;

    public FileUploadQueue(VivomoveHrCommunicator communicator) {
        this.communicator = communicator;
    }

    public void queueCreateFile(int fileSize, int dataType, int subType, int fileIdentifier, String targetPath, byte[] data) {
        queue.add(new QueueItem(fileSize, dataType, subType, fileIdentifier, targetPath, data));
        totalRemainingBytes += fileSize;
        checkStartNextUpload();
    }

    public void queueUploadFile(int fileSize, int fileIndex, byte[] data) {
        queue.add(new QueueItem(fileSize, fileIndex, data));
        totalRemainingBytes += fileSize;
        checkStartNextUpload();
    }

    public void cancelAllUploads() {
        queue.clear();
        currentlyUploadingItem = null;
//        communicator.sendMessage(new FileTransferDataResponseMessage(VivomoveConstants.STATUS_ACK, FileTransferDataResponseMessage.RESPONSE_ABORT_DOWNLOAD_REQUEST, 0).packet);
    }

    private boolean checkStartNextUpload() {
        if (currentlyUploadingItem != null) {
            LOG.debug("Another upload is pending");
            return false;
        }
        if (queue.isEmpty()) {
            LOG.debug("No upload in queue");
            return true;
        }
        startNextUpload();
        return false;
    }

    private void startNextUpload() {
        currentlyUploadingItem = queue.remove();
        currentCrc = 0;
        if (currentlyUploadingItem.create) {
            LOG.info("Requesting creation of '{}' ({}/{}/{}; {} B)", currentlyUploadingItem.targetPath, currentlyUploadingItem.dataType, currentlyUploadingItem.subType, currentlyUploadingItem.fileIdentifier, currentlyUploadingItem.fileSize);
            communicator.sendMessage(new CreateFileRequestMessage(currentlyUploadingItem.fileSize, currentlyUploadingItem.dataType, currentlyUploadingItem.subType, currentlyUploadingItem.fileIdentifier, 0, -1, currentlyUploadingItem.targetPath).packet);
        } else {
            LOG.info("Requesting upload of {} ({} B)", currentlyUploadingItem.fileIndex, currentlyUploadingItem.fileSize);
            communicator.sendMessage(new UploadRequestMessage(currentlyUploadingItem.fileIndex, 0, DownloadRequestMessage.REQUEST_NEW_TRANSFER, 0).packet);
        }
    }

    public void onCreateFileRequestResponse(CreateFileResponseMessage responseMessage) {
        if (currentlyUploadingItem == null) {
            LOG.error("Create file request response arrived, but nothing is being uploaded");
            return;
        }
        if (!currentlyUploadingItem.create) {
            LOG.error("Create file request response arrived, but nothing should have been created");
            return;
        }

        if (responseMessage.status == VivomoveConstants.STATUS_ACK && responseMessage.response == CreateFileResponseMessage.RESPONSE_FILE_CREATED_SUCCESSFULLY) {
            LOG.info("Received successful response for create file request of '{}' ({}/{}/{}; {} B) -> #{}", currentlyUploadingItem.targetPath, currentlyUploadingItem.dataType, currentlyUploadingItem.subType, currentlyUploadingItem.fileIdentifier, currentlyUploadingItem.fileSize, responseMessage.fileIndex);
            currentlyUploadingItem.fileIndex = responseMessage.fileIndex;
            communicator.sendMessage(new UploadRequestMessage(currentlyUploadingItem.fileIndex, 0, DownloadRequestMessage.REQUEST_NEW_TRANSFER, 0).packet);
        } else {
            LOG.error("Received error response for upload request request of '{}' ({}/{}/{}; {} B): {}, {}", currentlyUploadingItem.targetPath, currentlyUploadingItem.dataType, currentlyUploadingItem.subType, currentlyUploadingItem.fileIdentifier, currentlyUploadingItem.fileSize, responseMessage.status, responseMessage.response);
            totalRemainingBytes -= currentlyUploadingItem.fileSize;
            currentlyUploadingItem = null;
            checkStartNextUpload();
        }
    }

    public void onUploadRequestResponse(UploadRequestResponseMessage responseMessage) {
        if (currentlyUploadingItem == null) {
            LOG.error("Upload request response arrived, but nothing is being uploaded");
            return;
        }

        if (responseMessage.status == VivomoveConstants.STATUS_ACK && responseMessage.response == UploadRequestResponseMessage.RESPONSE_UPLOAD_REQUEST_OKAY) {
            LOG.info("Received successful response for upload request of {}: {}/{} (max {}B)", currentlyUploadingItem.fileIndex, responseMessage.status, responseMessage.response, responseMessage.maxFileSize);
            currentCrc = responseMessage.crcSeed;
        } else {
            LOG.error("Received error response for upload request of {}: {}/{}", currentlyUploadingItem.fileIndex, responseMessage.status, responseMessage.response);
            totalRemainingBytes -= currentlyUploadingItem.fileSize;
            currentlyUploadingItem = null;
            checkStartNextUpload();
        }
    }

    public void onFileTransferResponse(FileTransferDataResponseMessage dataResponseMessage) {
        final QueueItem currentlyUploadingItem = this.currentlyUploadingItem;
        if (currentlyUploadingItem == null) {
            LOG.error("Upload request response arrived, but nothing is being uploaded");
            return;
        }

        if (dataResponseMessage.status == VivomoveConstants.STATUS_ACK && dataResponseMessage.response == FileTransferDataResponseMessage.RESPONSE_TRANSFER_SUCCESSFUL) {
            int nextOffset = currentlyUploadingItem.dataOffset + currentlyUploadingItem.blockSize;
            if (dataResponseMessage.nextDataOffset != nextOffset) {
                LOG.warn("Bad expected data offset of #{}: {} expected, {} received", currentlyUploadingItem.fileIndex, currentlyUploadingItem.dataOffset, dataResponseMessage.nextDataOffset);
                communicator.sendMessage(new FileTransferDataResponseMessage(VivomoveConstants.STATUS_ACK, FileTransferDataResponseMessage.RESPONSE_ERROR_DATA_OFFSET_MISMATCH, currentlyUploadingItem.dataOffset).packet);
                return;
            }

            if (nextOffset >= currentlyUploadingItem.fileSize) {
                LOG.info("Transfer of file #{} complete, {}/{}B uploaded", currentlyUploadingItem.fileIndex, nextOffset, currentlyUploadingItem.fileSize);
                this.currentlyUploadingItem = null;
                checkStartNextUpload();
                return;
            }

            // prepare next block
            final int blockSize = Math.min(currentlyUploadingItem.fileSize - nextOffset, MAX_BLOCK_SIZE);
            currentlyUploadingItem.dataOffset = nextOffset;
            currentlyUploadingItem.blockSize = blockSize;
            final byte[] blockData = Arrays.copyOfRange(currentlyUploadingItem.data, nextOffset, blockSize);
            final int blockCrc = ChecksumCalculator.computeCrc(currentCrc, blockData, 0, blockSize);
            currentlyUploadingItem.blockCrc = blockCrc;

            LOG.info("Sending {}B@{}/{} of {}", blockSize, currentlyUploadingItem.dataOffset, currentlyUploadingItem.fileSize, currentlyUploadingItem.fileIndex);
            communicator.sendMessage(new FileTransferDataMessage(UPLOAD_FLAGS, blockCrc, currentlyUploadingItem.dataOffset, blockData).packet);
        } else {
            // TODO: Solve individual responses
            LOG.error("Received error response for data transfer of {}: {}/{}", currentlyUploadingItem.fileIndex, dataResponseMessage.status, dataResponseMessage.response);
            // ??!?
            cancelAllUploads();
        }
    }

    private boolean isIdle() {
        return currentlyUploadingItem == null;
    }

    private static class QueueItem {
        public final boolean create;
        public final int fileSize;
        public final int dataType;
        public final int subType;
        public final int fileIdentifier;
        public final String targetPath;
        public final byte[] data;

        public int fileIndex;
        public int dataOffset;
        public int blockSize;
        public int blockCrc;

        public QueueItem(int fileSize, int dataType, int subType, int fileIdentifier, String targetPath, byte[] data) {
            this.create = true;
            this.fileSize = fileSize;
            this.dataType = dataType;
            this.subType = subType;
            this.fileIdentifier = fileIdentifier;
            this.targetPath = targetPath;
            this.data = data;
        }

        public QueueItem(int fileSize, int fileIndex, byte[] data) {
            this.create = false;
            this.fileSize = fileSize;
            this.fileIndex = fileIndex;
            this.data = data;
            this.dataType = 0;
            this.subType = 0;
            this.fileIdentifier = 0;
            this.targetPath = null;
        }
    }
}

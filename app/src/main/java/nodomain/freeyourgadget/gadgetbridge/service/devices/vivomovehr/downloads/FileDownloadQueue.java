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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.VivomoveHrCommunicator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.DownloadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.DownloadRequestResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FileTransferDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FileTransferDataResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class FileDownloadQueue {
    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadQueue.class);

    private final VivomoveHrCommunicator communicator;
    private final FileDownloadListener listener;

    private final Queue<QueueItem> queue = new LinkedList<>();
    private final Set<Integer> queuedFileIndices = new HashSet<>();

    private QueueItem currentlyDownloadingItem;
    private int currentCrc;
    private long totalRemainingBytes;

    public FileDownloadQueue(VivomoveHrCommunicator communicator, FileDownloadListener listener) {
        this.communicator = communicator;
        this.listener = listener;
    }

    public void addToDownloadQueue(int fileIndex, int dataSize) {
        if (queuedFileIndices.contains(fileIndex)) {
            LOG.debug("Ignoring download request of {}, already in queue", fileIndex);
            return;
        }
        queue.add(new QueueItem(fileIndex, dataSize));
        queuedFileIndices.add(fileIndex);
        totalRemainingBytes += dataSize;
        checkRequestNextDownload();
    }

    public void cancelAllDownloads() {
        queue.clear();
        currentlyDownloadingItem = null;
        communicator.sendMessage(new FileTransferDataResponseMessage(VivomoveConstants.STATUS_ACK, FileTransferDataResponseMessage.RESPONSE_ABORT_DOWNLOAD_REQUEST, 0).packet);
    }

    private boolean checkRequestNextDownload() {
        if (currentlyDownloadingItem != null) {
            LOG.debug("Another download is pending");
            return false;
        }
        if (queue.isEmpty()) {
            LOG.debug("No download in queue");
            return true;
        }
        requestNextDownload();
        return false;
    }

    private void requestNextDownload() {
        currentlyDownloadingItem = queue.remove();
        currentCrc = 0;
        final int fileIndex = currentlyDownloadingItem.fileIndex;
        LOG.info("Requesting download of {} ({} B)", fileIndex, currentlyDownloadingItem.dataSize);
        queuedFileIndices.remove(fileIndex);
        communicator.sendMessage(new DownloadRequestMessage(fileIndex, 0, DownloadRequestMessage.REQUEST_NEW_TRANSFER, 0, 0).packet);
    }

    public void onDownloadRequestResponse(DownloadRequestResponseMessage responseMessage) {
        if (currentlyDownloadingItem == null) {
            LOG.error("Download request response arrived, but nothing is being downloaded");
            return;
        }

        if (responseMessage.status == VivomoveConstants.STATUS_ACK && responseMessage.response == DownloadRequestResponseMessage.RESPONSE_DOWNLOAD_REQUEST_OKAY) {
            LOG.info("Received response for download request of {}: {}/{}, {}B", currentlyDownloadingItem.fileIndex, responseMessage.status, responseMessage.response, responseMessage.fileSize);
            totalRemainingBytes += responseMessage.fileSize - currentlyDownloadingItem.dataSize;
            currentlyDownloadingItem.setDataSize(responseMessage.fileSize);
        } else {
            LOG.error("Received error response for download request of {}: {}/{}", currentlyDownloadingItem.fileIndex, responseMessage.status, responseMessage.response);
            listener.onFileDownloadError(currentlyDownloadingItem.fileIndex);
            totalRemainingBytes -= currentlyDownloadingItem.dataSize;
            currentlyDownloadingItem = null;
            checkRequestNextDownload();
        }
    }

    public void onFileTransferData(FileTransferDataMessage dataMessage) {
        final QueueItem currentlyDownloadingItem = this.currentlyDownloadingItem;
        if (currentlyDownloadingItem == null) {
            LOG.error("Download request response arrived, but nothing is being downloaded");
            communicator.sendMessage(new FileTransferDataResponseMessage(VivomoveConstants.STATUS_ACK, FileTransferDataResponseMessage.RESPONSE_ABORT_DOWNLOAD_REQUEST, 0).packet);
            return;
        }

        if (dataMessage.dataOffset < currentlyDownloadingItem.dataOffset) {
            LOG.warn("Ignoring repeated transfer at offset {} of #{}", dataMessage.dataOffset, currentlyDownloadingItem.fileIndex);
            communicator.sendMessage(new FileTransferDataResponseMessage(VivomoveConstants.STATUS_ACK, FileTransferDataResponseMessage.RESPONSE_ERROR_DATA_OFFSET_MISMATCH, currentlyDownloadingItem.dataOffset).packet);
            return;
        }
        if (dataMessage.dataOffset > currentlyDownloadingItem.dataOffset) {
            LOG.warn("Missing data at offset {} when received data at offset {} of #{}", currentlyDownloadingItem.dataOffset, dataMessage.dataOffset, currentlyDownloadingItem.fileIndex);
            communicator.sendMessage(new FileTransferDataResponseMessage(VivomoveConstants.STATUS_ACK, FileTransferDataResponseMessage.RESPONSE_ERROR_DATA_OFFSET_MISMATCH, currentlyDownloadingItem.dataOffset).packet);
            return;
        }

        final int dataCrc = ChecksumCalculator.computeCrc(currentCrc, dataMessage.data, 0, dataMessage.data.length);
        if (dataCrc != dataMessage.crc) {
            LOG.warn("Invalid CRC ({} vs {}) for {}B data @{} of {}", dataCrc, dataMessage.crc, dataMessage.data.length, dataMessage.dataOffset, currentlyDownloadingItem.fileIndex);
            communicator.sendMessage(new FileTransferDataResponseMessage(VivomoveConstants.STATUS_ACK, FileTransferDataResponseMessage.RESPONSE_ERROR_CRC_MISMATCH, currentlyDownloadingItem.dataOffset).packet);
            return;
        }
        currentCrc = dataCrc;

        LOG.info("Received {}B@{}/{} of {}", dataMessage.data.length, dataMessage.dataOffset, currentlyDownloadingItem.dataSize, currentlyDownloadingItem.fileIndex);
        currentlyDownloadingItem.appendData(dataMessage.data);
        communicator.sendMessage(new FileTransferDataResponseMessage(VivomoveConstants.STATUS_ACK, FileTransferDataResponseMessage.RESPONSE_TRANSFER_SUCCESSFUL, currentlyDownloadingItem.dataOffset).packet);

        totalRemainingBytes -= dataMessage.data.length;
        listener.onDownloadProgress(totalRemainingBytes);

        if (currentlyDownloadingItem.dataOffset >= currentlyDownloadingItem.dataSize) {
            LOG.info("Transfer of file #{} complete, {}/{}B downloaded", currentlyDownloadingItem.fileIndex, currentlyDownloadingItem.dataOffset, currentlyDownloadingItem.dataSize);
            this.currentlyDownloadingItem = null;
            final boolean allDone = checkRequestNextDownload();
            reportCompletedDownload(currentlyDownloadingItem);
            if (allDone && isIdle()) listener.onAllDownloadsCompleted();
        }
    }

    private boolean isIdle() {
        return currentlyDownloadingItem == null;
    }

    private void reportCompletedDownload(QueueItem downloadedItem) {
        if (downloadedItem.fileIndex == 0) {
            final DirectoryData directoryData = DirectoryData.parse(downloadedItem.data);
            listener.onDirectoryDownloaded(directoryData);
        } else {
            listener.onFileDownloadComplete(downloadedItem.fileIndex, downloadedItem.data);
        }
    }

    private static class QueueItem {
        public final int fileIndex;
        public int dataSize;
        public int dataOffset;
        public byte[] data;

        public QueueItem(int fileIndex, int dataSize) {
            this.fileIndex = fileIndex;
            this.dataSize = dataSize;
        }

        public void setDataSize(int dataSize) {
            if (this.data != null) throw new IllegalStateException("Data size already set");
            this.dataSize = dataSize;
            this.data = new byte[dataSize];
        }

        public void appendData(byte[] data) {
            System.arraycopy(data, 0, this.data, dataOffset, data.length);
            dataOffset += data.length;
        }
    }
}

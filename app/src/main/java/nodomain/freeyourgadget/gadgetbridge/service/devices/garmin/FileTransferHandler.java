package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.DownloadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FileTransferDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SystemEventMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.UploadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.CreateFileStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.DownloadRequestStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FileTransferDataStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.UploadRequestStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class FileTransferHandler implements MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FileTransferHandler.class);
    private final GarminSupport deviceSupport;
    private final Download download;
    private final Upload upload;

    public FileTransferHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
        this.download = new Download();
        this.upload = new Upload();
    }

    public boolean isDownloading() {
        return download.getCurrentlyDownloading() != null;
    }

    public boolean isUploading() {
        return upload.getCurrentlyUploading() != null;
    }

    public GFDIMessage handle(GFDIMessage message) {
        if (message instanceof DownloadRequestStatusMessage)
            download.processDownloadRequestStatusMessage((DownloadRequestStatusMessage) message);
        else if (message instanceof FileTransferDataMessage)
            download.processDownloadChunkedMessage((FileTransferDataMessage) message);
        else if (message instanceof CreateFileStatusMessage)
            return upload.setCreateFileStatusMessage((CreateFileStatusMessage) message);
        else if (message instanceof UploadRequestStatusMessage)
            return upload.setUploadRequestStatusMessage((UploadRequestStatusMessage) message);
        else if (message instanceof FileTransferDataStatusMessage)
            return upload.processUploadProgress((FileTransferDataStatusMessage) message);

        return null;
    }

    public DownloadRequestMessage downloadDirectoryEntry(DirectoryEntry directoryEntry) {
        download.setCurrentlyDownloading(new FileFragment(directoryEntry));
        return new DownloadRequestMessage(directoryEntry.getFileIndex(), 0, DownloadRequestMessage.REQUEST_TYPE.NEW, 0, 0);
    }

    public DownloadRequestMessage initiateDownload() {
        download.setCurrentlyDownloading(new FileFragment(new DirectoryEntry(0, FileType.FILETYPE.DIRECTORY, 0, 0, 0, 0, null)));
        return new DownloadRequestMessage(0, 0, DownloadRequestMessage.REQUEST_TYPE.NEW, 0, 0);
    }
//    public DownloadRequestMessage downloadSettings() {
//        download.setCurrentlyDownloading(new FileFragment(new DirectoryEntry(0, FileType.FILETYPE.SETTINGS, 0, 0, 0, 0, null)));
//        return new DownloadRequestMessage(0, 0, DownloadRequestMessage.REQUEST_TYPE.NEW, 0, 0);
//    }
//
//    public CreateFileMessage initiateUpload(byte[] fileAsByteArray, FileType.FILETYPE filetype) {
//        upload.setCurrentlyUploading(new FileFragment(new DirectoryEntry(0, filetype, 0, 0, 0, fileAsByteArray.length, null), fileAsByteArray));
//        return new CreateFileMessage(fileAsByteArray.length, filetype);
//    }


    public class Download {
        private FileFragment currentlyDownloading;

        public FileFragment getCurrentlyDownloading() {
            return currentlyDownloading;
        }

        public void setCurrentlyDownloading(FileFragment currentlyDownloading) {
            this.currentlyDownloading = currentlyDownloading;
        }

        private void processDownloadChunkedMessage(FileTransferDataMessage fileTransferDataMessage) {
            if (!isDownloading())
                throw new IllegalStateException("Received file transfer of unknown file");

            currentlyDownloading.append(fileTransferDataMessage);
            if (!currentlyDownloading.dataHolder.hasRemaining())
                processCompleteDownload();
        }

        private void processCompleteDownload() {
            currentlyDownloading.dataHolder.flip();

            if (FileType.FILETYPE.DIRECTORY.equals(currentlyDownloading.directoryEntry.filetype)) { //is a directory
                parseDirectoryEntries();
            } else {
                saveFileToExternalStorage();
            }

            currentlyDownloading = null;
        }

        public void processDownloadRequestStatusMessage(DownloadRequestStatusMessage downloadRequestStatusMessage) {
            if (null == currentlyDownloading)
                throw new IllegalStateException("Received file transfer of unknown file");
            if (downloadRequestStatusMessage.canProceed())
                currentlyDownloading.setSize(downloadRequestStatusMessage);
            else
                currentlyDownloading = null;
        }

        private void saveFileToExternalStorage() {
            File dir;
            try {
                dir = deviceSupport.getWritableExportDirectory();
                File outputFile = new File(dir, currentlyDownloading.getFileName());
                FileUtils.copyStreamToFile(new ByteArrayInputStream(currentlyDownloading.dataHolder.array()), outputFile);
                outputFile.setLastModified(currentlyDownloading.directoryEntry.fileDate.getTime());

            } catch (IOException e) {
                LOG.error("Failed to save file", e);
            }

            LOG.debug("FILE DOWNLOAD COMPLETE {}", currentlyDownloading.getFileName());
        }

        private void parseDirectoryEntries() {
            if ((currentlyDownloading.getDataSize() % 16) != 0)
                throw new IllegalArgumentException("Invalid directory data length");
            final GarminByteBufferReader reader = new GarminByteBufferReader(currentlyDownloading.dataHolder.array());
            reader.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            while (reader.remaining() > 0) {
                final int fileIndex = reader.readShort();//2
                final int fileDataType = reader.readByte();//3
                final int fileSubType = reader.readByte();//4
                final FileType.FILETYPE filetype = FileType.FILETYPE.fromDataTypeSubType(fileDataType, fileSubType);
                final int fileNumber = reader.readShort();//6
                final int specificFlags = reader.readByte();//7
                final int fileFlags = reader.readByte();//8
                final int fileSize = reader.readInt();//12
                final Date fileDate = new Date(GarminTimeUtils.garminTimestampToJavaMillis(reader.readInt()));//16
                final DirectoryEntry directoryEntry = new DirectoryEntry(fileIndex, filetype, fileNumber, specificFlags, fileFlags, fileSize, fileDate);
                if (directoryEntry.filetype == null) //silently discard unsupported files
                    continue;
                deviceSupport.addFileToDownloadList(directoryEntry);
            }
            currentlyDownloading = null;
        }
    }

    public static class Upload {
        private FileFragment currentlyUploading;

        private UploadRequestMessage setCreateFileStatusMessage(CreateFileStatusMessage createFileStatusMessage) {
            if (createFileStatusMessage.canProceed()) {
                LOG.info("SENDING UPLOAD FILE");
                return new UploadRequestMessage(createFileStatusMessage.getFileIndex(), currentlyUploading.getDataSize());
            } else {
                LOG.warn("Cannot proceed with upload");
                this.currentlyUploading = null;
            }
            return null;
        }

        private FileTransferDataMessage setUploadRequestStatusMessage(UploadRequestStatusMessage uploadRequestStatusMessage) {
            if (null == currentlyUploading)
                throw new IllegalStateException("Received upload request status transfer of unknown file");
            if (uploadRequestStatusMessage.canProceed()) {
                if (uploadRequestStatusMessage.getDataOffset() != currentlyUploading.dataHolder.position())
                    throw new IllegalStateException("Received upload request with unaligned offset");
                return currentlyUploading.take();
            } else {
                LOG.warn("Cannot proceed with upload");
                this.currentlyUploading = null;
            }
            return null;
        }

        private GFDIMessage processUploadProgress(FileTransferDataStatusMessage fileTransferDataStatusMessage) {
            if (currentlyUploading.getDataSize() <= fileTransferDataStatusMessage.getDataOffset()) {
                this.currentlyUploading = null;
                LOG.info("SENDING SYNC COMPLETE!!!");

                return new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_COMPLETE, 0);
            } else {
                if (fileTransferDataStatusMessage.canProceed()) {
                    LOG.info("SENDING NEXT CHUNK!!!");
                    if (fileTransferDataStatusMessage.getDataOffset() != currentlyUploading.dataHolder.position())
                        throw new IllegalStateException("Received file transfer status with unaligned offset");
                    return currentlyUploading.take();
                } else {
                    LOG.warn("Cannot proceed with upload");
                    this.currentlyUploading = null;
                }

            }
            return null;
        }

        public FileFragment getCurrentlyUploading() {
            return this.currentlyUploading;
        }

        public void setCurrentlyUploading(FileFragment currentlyUploading) {
            this.currentlyUploading = currentlyUploading;
        }

    }

    public static class FileFragment {
        private final DirectoryEntry directoryEntry;
        private final int maxBlockSize = 500;
        private int dataSize;
        private ByteBuffer dataHolder;
        private int runningCrc;

        FileFragment(DirectoryEntry directoryEntry) {
            this.directoryEntry = directoryEntry;
            this.setRunningCrc(0);
        }

        FileFragment(DirectoryEntry directoryEntry, byte[] contents) {
            this.directoryEntry = directoryEntry;
            this.setDataSize(contents.length);
            this.dataHolder = ByteBuffer.wrap(contents);
            this.dataHolder.flip(); //we'll be only reading from here on
            this.dataHolder.compact();
            this.setRunningCrc(0);
        }

        private int getMaxBlockSize() {
            return Math.max(maxBlockSize, GFDIMessage.getMaxPacketSize());
        }

        public String getFileName() {
            return directoryEntry.getFileName();
        }

        private void setSize(DownloadRequestStatusMessage downloadRequestStatusMessage) {
            if (0 != getDataSize())
                throw new IllegalStateException("Data size already set");

            this.setDataSize(downloadRequestStatusMessage.getMaxFileSize());
            this.dataHolder = ByteBuffer.allocate(getDataSize());
        }

        private void append(FileTransferDataMessage fileTransferDataMessage) {
            if (fileTransferDataMessage.getDataOffset() != dataHolder.position())
                throw new IllegalStateException("Received message that was already received");

            final int dataCrc = ChecksumCalculator.computeCrc(getRunningCrc(), fileTransferDataMessage.getMessage(), 0, fileTransferDataMessage.getMessage().length);
            if (fileTransferDataMessage.getCrc() != dataCrc)
                throw new IllegalStateException("Received message with invalid CRC");
            setRunningCrc(dataCrc);

            this.dataHolder.put(fileTransferDataMessage.getMessage());
        }

        private FileTransferDataMessage take() {
            final int currentOffset = this.dataHolder.position();
            final byte[] chunk = new byte[Math.min(this.dataHolder.remaining(), getMaxBlockSize())];
            this.dataHolder.get(chunk);
            setRunningCrc(ChecksumCalculator.computeCrc(getRunningCrc(), chunk, 0, chunk.length));
            return new FileTransferDataMessage(chunk, currentOffset, getRunningCrc());
        }

        private int getDataSize() {
            return dataSize;
        }

        private void setDataSize(int dataSize) {
            this.dataSize = dataSize;
        }

        private int getRunningCrc() {
            return runningCrc;
        }

        private void setRunningCrc(int runningCrc) {
            this.runningCrc = runningCrc;
        }
    }

    public static class DirectoryEntry {
        private final int fileIndex;
        private final FileType.FILETYPE filetype;
        private final int fileNumber;
        private final int specificFlags;
        private final int fileFlags;
        private final int fileSize;
        private final Date fileDate;

        public DirectoryEntry(int fileIndex, FileType.FILETYPE filetype, int fileNumber, int specificFlags, int fileFlags, int fileSize, Date fileDate) {
            this.fileIndex = fileIndex;
            this.filetype = filetype;
            this.fileNumber = fileNumber;
            this.specificFlags = specificFlags;
            this.fileFlags = fileFlags;
            this.fileSize = fileSize;
            this.fileDate = fileDate;
        }

        public int getFileIndex() {
            return fileIndex;
        }

        public FileType.FILETYPE getFiletype() {
            return filetype;
        }

        public String getFileName() {
            return getFiletype().name() + "_" + getFileIndex() + (getFiletype().isFitFile() ? ".fit" : "");
        }

        @NonNull
        @Override
        public String toString() {
            return "DirectoryEntry{" +
                    "fileIndex=" + fileIndex +
                    ", fileType=" + filetype.name() +
                    ", fileNumber=" + fileNumber +
                    ", specificFlags=" + specificFlags +
                    ", fileFlags=" + fileFlags +
                    ", fileSize=" + fileSize +
                    ", fileDate=" + fileDate +
                    '}';
        }
    }
}

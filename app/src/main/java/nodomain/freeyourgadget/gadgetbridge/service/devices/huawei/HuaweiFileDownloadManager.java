/*  Copyright (C) 2024 Martin.JM

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

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileDownloadService0A;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileDownloadService2C;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetFileBlockRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetFileDownloadCompleteRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetFileDownloadInitRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetFileInfoRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetFileParametersRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiFileDownloadManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiFileDownloadManager.class);

    public static class HuaweiFileDownloadException extends Exception {
        @Nullable
        public final FileRequest fileRequest;

        HuaweiFileDownloadException(@Nullable FileRequest fileRequest, String message) {
            super(message);
            this.fileRequest = fileRequest;
        }

        HuaweiFileDownloadException(@Nullable FileRequest fileRequest, String message, Exception e) {
            super(message, e);
            this.fileRequest = fileRequest;
        }
    }

    public static class HuaweiFileDownloadSendException extends HuaweiFileDownloadException {
        HuaweiFileDownloadSendException(@Nullable FileRequest fileRequest, Request request, Exception e) {
            super(fileRequest, "Error sending request " + request.getName(), e);
        }
    }

    public static class HuaweiFileDownloadRequestException extends HuaweiFileDownloadException {
        HuaweiFileDownloadRequestException(@Nullable FileRequest fileRequest, Class<?> requestClass, Exception e) {
            super(fileRequest, "Error in request of class " + requestClass, e);
        }
    }

    public static class HuaweiFileDownloadTimeoutException extends HuaweiFileDownloadException {
        HuaweiFileDownloadTimeoutException(@Nullable FileRequest fileRequest) {
            super(fileRequest, "Timeout was hit!");
        }
    }

    public static class HuaweiFileDownloadFileMismatchException extends HuaweiFileDownloadException {
        HuaweiFileDownloadFileMismatchException(@NonNull FileRequest fileRequest, String filename) {
            super(fileRequest, "Data for wrong file received. Expected name " + fileRequest.filename + ", got name " + filename);
        }

        HuaweiFileDownloadFileMismatchException(@NonNull FileRequest fileRequest, FileType fileType) {
            super(fileRequest, "Data for wrong file received. Expected type " + fileRequest.fileType + ", got type " + fileType);
        }

        HuaweiFileDownloadFileMismatchException(@NonNull FileRequest fileRequest, String[] filenames) {
            super(fileRequest, "File " + fileRequest.filename + " cannot be downloaded using this method. Only the following files are supported: " + Arrays.toString(filenames));
        }

        HuaweiFileDownloadFileMismatchException(@NonNull FileRequest fileRequest, int number, boolean newSync) {
            super(
                    fileRequest,
                    "Data for wrong file received. Expected " +
                        (newSync ?
                            "id " + fileRequest.fileId + ", got id " + number :
                            "packet number " + (fileRequest.lastPacketNumber + 1) + ", got " + number)
            );
        }
    }

    public enum FileType {
        DEBUG,
        SLEEP_STATE,
        SLEEP_DATA,
        GPS,
        UNKNOWN // Never for input!
    }

    public static class FileDownloadCallback {
        public void downloadComplete(FileRequest fileRequest) {  }

        public void downloadException(HuaweiFileDownloadException e) {
            if (e.fileRequest != null)
                LOG.error("Error downloading file: {}{}", e.fileRequest.getFilename(), e.fileRequest.isNewSync() ? " (newsync)" : "", e);
            else
                LOG.error("Error in file download", e);
        }
    }

    public static class FileRequest {
        // Inputs

        private final String filename;
        private final FileType fileType;
        private final boolean newSync;

        FileDownloadCallback fileDownloadCallback = null;

        // Sleep type only - for 2C GPS they are set to zero
        private int startTime = 0;
        private int endTime = 0;

        // GPS type only
        private short workoutId;
        private Long databaseId;

        private FileRequest(String filename, FileType fileType, boolean newSync, int startTime, int endTime, FileDownloadCallback fileDownloadCallback) {
            this.filename = filename;
            this.fileType = fileType;
            this.newSync = newSync;
            this.fileDownloadCallback = fileDownloadCallback;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public static FileRequest sleepStateFileRequest(boolean supportsTruSleepNewSync, int startTime, int endTime, FileDownloadCallback fileDownloadCallback) {
            return new FileRequest("sleep_state.bin", FileType.SLEEP_STATE, supportsTruSleepNewSync, startTime, endTime, fileDownloadCallback);
        }

        public static FileRequest sleepDataFileRequest(boolean supportsTruSleepNewSync, int startTime, int endTime, FileDownloadCallback fileDownloadCallback) {
            return new FileRequest("sleep_data.bin", FileType.SLEEP_DATA, supportsTruSleepNewSync, startTime, endTime, fileDownloadCallback);
        }

        private FileRequest(String filename, FileType fileType, boolean newSync, FileDownloadCallback fileDownloadCallback) {
            this.filename = filename;
            this.fileType = fileType;
            this.newSync = newSync;
            this.fileDownloadCallback = fileDownloadCallback;
        }

        public static FileRequest debugFileRequest(String filename, FileDownloadCallback fileDownloadCallback) {
            return new FileRequest(filename, FileType.DEBUG, false, fileDownloadCallback);
        }

        private FileRequest(@Nullable String filename, FileType fileType, boolean newSync, short workoutId, Long databaseId, FileDownloadCallback fileDownloadCallback) {
            this.filename = filename;
            this.fileType = fileType;
            this.newSync = newSync;
            this.fileDownloadCallback = fileDownloadCallback;
            this.workoutId = workoutId;
            this.databaseId = databaseId;
        }

        public static FileRequest workoutGpsFileRequest(boolean newSync, short workoutId, Long databaseId, FileDownloadCallback fileDownloadCallback) {
            if (newSync)
                return new FileRequest(String.format(Locale.getDefault(), "%d_gps.bin", workoutId), FileType.GPS, true, workoutId, databaseId, fileDownloadCallback);
            else
                return new FileRequest(null, FileType.GPS, false, workoutId, databaseId, fileDownloadCallback);
        }

        // Retrieved

        private int fileSize;
        private int maxBlockSize;
        private int timeout; // TODO: unit?
        private ByteBuffer buffer;

        private int startOfBlockOffset;
        private int currentBlockSize;

        // Old sync only
        private String[] filenames;
        private byte lastPacketNumber;

        // New sync only
        private byte fileId;
        private boolean noEncrypt;

        public byte getFileId() {
            return fileId;
        }

        public String getFilename() {
            return filename;
        }

        public FileType getFileType() {
            return fileType;
        }

        public byte[] getData() {
            if (buffer == null)
                return new byte[] {};
            return buffer.array();
        }

        public int getCurrentOffset() {
            return buffer.position();
        }

        public boolean isNewSync() {
            return newSync;
        }

        public int getStartTime() {
            return startTime;
        }

        public int getEndTime() {
            return endTime;
        }

        public short getWorkoutId() {
            return workoutId;
        }

        public Long getDatabaseId() {
            return databaseId;
        }

        public int getCurrentBlockSize() {
            return currentBlockSize;
        }

        public boolean isNoEncrypt() {
            return noEncrypt;
        }
    }

    /**
     * Actually receives the file data
     */
    private static class FileDataReceiver extends Request {

        public boolean newSync;
        public byte[] data;

        // Packet number for old sync
        // fileId for new sync
        public byte number;

        public FileDataReceiver(HuaweiSupportProvider supportProvider) {
            super(supportProvider);
        }

        @Override
        public boolean handleResponse(HuaweiPacket response) {
            if (
                    (response.serviceId == FileDownloadService0A.id &&
                        response.commandId == FileDownloadService0A.BlockResponse.id) ||
                    (response.serviceId == FileDownloadService2C.id &&
                        response.commandId == FileDownloadService2C.BlockResponse.id)
            ) {
                receivedPacket = response;
                return true;
            }
            return false;
        }

        @Override
        public boolean autoRemoveFromResponseHandler() {
            // This needs to be removed manually
            return false;
        }

        @Override
        protected void processResponse() throws ResponseParseException {
            if (this.receivedPacket instanceof FileDownloadService0A.BlockResponse) {
                FileDownloadService0A.BlockResponse response = (FileDownloadService0A.BlockResponse) this.receivedPacket;
                this.newSync = false;
                this.number = response.number;
                this.data = response.data;

                if (response.e instanceof HuaweiPacket.CryptoException) {
                    LOG.warn("Data could be decoded as TLV, but not decrypted.", response.e);
                }
            } else if (this.receivedPacket instanceof FileDownloadService2C.BlockResponse) {
                this.newSync = true;
                this.number = ((FileDownloadService2C.BlockResponse) this.receivedPacket).fileId;
                this.data = ((FileDownloadService2C.BlockResponse) this.receivedPacket).data;
            } else {
                throw new ResponseTypeMismatchException(
                        this.receivedPacket,
                        FileDownloadService0A.BlockResponse.class,
                        FileDownloadService2C.BlockResponse.class
                );
            }
        }
    }

    private final HuaweiSupportProvider supportProvider;
    private final Handler handler;
    private final Runnable timeout;

    // Cannot be final as we need the device to be connected before creating this
    private FileDataReceiver fileDataReceiver;

    // For old sync we cannot download multiple files at the same time, for new sync we don't want
    // to, so we limit that. We also do not allow old and new sync at the same time.
    // Note that old and new sync are already split by the serviceID and commandID that are used for
    // all the requests.
    // Note that the timeout is not ready for concurrent downloads
    private boolean isBusy;

    private final ArrayList<FileRequest> fileRequests;
    private FileRequest currentFileRequest;

    // If the GB interface needs to be updated at the end of the queue of downloads
    private boolean needSync = false;

    public HuaweiFileDownloadManager(HuaweiSupportProvider supportProvider) {
        this.supportProvider = supportProvider;
        handler = new Handler(Looper.getMainLooper());
        isBusy = false;
        fileRequests = new ArrayList<>();
        timeout = () -> {
            this.currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadTimeoutException(currentFileRequest));
            reset();
        };
    }

    public void addToQueue(FileRequest fileRequest, boolean needSync) {
        synchronized (supportProvider) {
            fileRequests.add(fileRequest);
            if (needSync)
                this.needSync = true;
        }
        startDownload();
    }

    private boolean arrayContains(String[] haystack, String needle) {
        return Arrays.stream(haystack).anyMatch(s -> s.equals(needle));
    }

    private void initFileDataReceiver() {
        if (fileDataReceiver == null) {
            // We can only init fileDataReceiver if the device is already connected
            fileDataReceiver = new FileDataReceiver(supportProvider);
            fileDataReceiver.setFinalizeReq(new Request.RequestCallback() {
                @Override
                public void call() {
                    // Reset timeout
                    handler.removeCallbacks(HuaweiFileDownloadManager.this.timeout);
                    handler.postDelayed(HuaweiFileDownloadManager.this.timeout, currentFileRequest.timeout * 1000L);

                    // Handle data
                    handleFileData(fileDataReceiver.newSync, fileDataReceiver.number, fileDataReceiver.data);
                }

                @Override
                public void handleException(Request.ResponseParseException e) {
                    currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadRequestException(null, this.getClass(), e));
                }
            });
        }
    }

    public void startDownload() {
        initFileDataReceiver(); // Make sure the fileDataReceiver is ready

        synchronized (this.supportProvider) {
            if (this.isBusy) {
                LOG.debug("A new download is started while a previous is in progress.");
                return; // Already downloading, this file will come eventually
            }
            if (this.fileRequests.isEmpty()) {
                // No more files to download
                supportProvider.downloadQueueEmpty(this.needSync);
                // Don't need sync after this anymore
                this.needSync = false;
                return;
            }
            this.isBusy = true;
        }

        this.currentFileRequest = this.fileRequests.remove(0);

        GetFileDownloadInitRequest getFileDownloadInitRequest = new GetFileDownloadInitRequest(supportProvider, currentFileRequest);
        getFileDownloadInitRequest.setFinalizeReq(new Request.RequestCallback() {
            @Override
            public void call(Request request) {
                // For multi-download, match to file instead of assuming current
                GetFileDownloadInitRequest r = (GetFileDownloadInitRequest) request;
                if (r.newSync) {
                    if (!currentFileRequest.filename.equals(r.filename)) {
                        currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadFileMismatchException(
                                currentFileRequest,
                                r.filename
                        ));
                        reset();
                        return;
                    }
                    if (currentFileRequest.fileType != r.fileType) {
                        currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadFileMismatchException(
                                currentFileRequest,
                                r.fileType
                        ));
                        reset();
                        return;
                    }
                    currentFileRequest.fileId = r.fileId;
                    currentFileRequest.fileSize = r.fileSize;
                    if (r.fileSize == 0) {
                        // Nothing to download, go to end
                        fileComplete();
                        return;
                    }
                    getFileInfo();
                } else {
                    if (currentFileRequest.fileType == FileType.GPS) {
                        if (r.filenames.length == 0) {
                            reset();
                            return;
                        }
                        for (String filename : r.filenames) {
                            // We only download the gps file itself right now
                            if (!filename.contains("_gps.bin"))
                                continue;

                            // Add download request with the filename at the start of the queue
                            FileRequest fileRequest = new FileRequest(
                                    filename,
                                    FileType.GPS,
                                    currentFileRequest.newSync,
                                    currentFileRequest.workoutId,
                                    currentFileRequest.databaseId,
                                    currentFileRequest.fileDownloadCallback
                            );
                            synchronized (supportProvider) {
                                fileRequests.add(0, fileRequest);
                            }
                        }
                        currentFileRequest = fileRequests.remove(0); // Replace with the file to download
                    } else {
                        if (!arrayContains(r.filenames, currentFileRequest.filename)) {
                            currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadFileMismatchException(
                                    currentFileRequest,
                                    r.filenames
                            ));
                            reset();
                            return;
                        }
                    }
                    currentFileRequest.filenames = r.filenames;
                    getDownloadParameters();
                }
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadRequestException(currentFileRequest, this.getClass(), e));
            }
        });
        try {
            getFileDownloadInitRequest.doPerform();
        } catch (IOException e) {
            currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileDownloadInitRequest, e));
            reset();
        }
    }

    private void getDownloadParameters() {
        // Old sync only, can never be multiple at the same time
        // Assuming currentRequest is the correct one the entire time
        // Which may no longer be the case when we implement multi-download for new sync
        GetFileParametersRequest getFileParametersRequest = new GetFileParametersRequest(supportProvider,
                currentFileRequest.fileType == FileType.SLEEP_STATE ||
                        currentFileRequest.fileType == FileType.SLEEP_DATA
        );
        getFileParametersRequest.setFinalizeReq(new Request.RequestCallback() {
            @Override
            public void call() {
                currentFileRequest.maxBlockSize = getFileParametersRequest.getMaxBlockSize();
                currentFileRequest.timeout = getFileParametersRequest.getTimeout();
                getFileInfo();
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadRequestException(null, this.getClass(), e));
                reset();
            }
        });
        try {
            getFileParametersRequest.doPerform();
        } catch (IOException e) {
            currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileParametersRequest, e));
            reset();
        }
    }

    private void getFileInfo() {
        GetFileInfoRequest getFileInfoRequest = new GetFileInfoRequest(supportProvider, currentFileRequest);
        getFileInfoRequest.setFinalizeReq(new Request.RequestCallback() {
            @Override
            public void call(Request request) {
                GetFileInfoRequest r = (GetFileInfoRequest) request;
                if (r.newSync) {
                    if (currentFileRequest.fileId != r.fileId) {
                        currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadFileMismatchException(currentFileRequest, r.fileId, true));
                        reset();
                        return;
                    }
                    currentFileRequest.timeout = r.timeout;
                    currentFileRequest.maxBlockSize = r.maxBlockSize;
                    currentFileRequest.noEncrypt = r.noEncrypt;
                } else {
                    // currentFileRequest MUST BE correct here
                    currentFileRequest.fileSize = r.fileLength;

                    if (currentFileRequest.fileSize == 0) {
                        // Nothing to download, go to complete
                        fileComplete();
                        return;
                    }
                }
                downloadNextFileBlock();
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadRequestException(null, this.getClass(), e));
                reset();
            }
        });
        try {
            getFileInfoRequest.doPerform();
        } catch (IOException e) {
            currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileInfoRequest, e));
            reset();
        }
    }

    private void downloadNextFileBlock() {
        if (currentFileRequest.buffer == null) // New file
            currentFileRequest.buffer = ByteBuffer.allocate(currentFileRequest.fileSize);
        currentFileRequest.lastPacketNumber = -1; // Counts per block
        currentFileRequest.startOfBlockOffset = currentFileRequest.buffer.position();
        currentFileRequest.currentBlockSize = Math.min(
                currentFileRequest.fileSize - currentFileRequest.buffer.position(), // Remaining file size
                currentFileRequest.maxBlockSize // Max we can ask for
        );

        // Start listening for file data
        this.supportProvider.addInProgressRequest(fileDataReceiver);

        handler.removeCallbacks(this.timeout);
        handler.postDelayed(HuaweiFileDownloadManager.this.timeout, currentFileRequest.timeout * 1000L);

        GetFileBlockRequest getFileBlockRequest = new GetFileBlockRequest(supportProvider, currentFileRequest);
        getFileBlockRequest.setFinalizeReq(new Request.RequestCallback() {
            @Override
            public void call() {
                // Reset timeout
                handler.removeCallbacks(HuaweiFileDownloadManager.this.timeout);
                handler.postDelayed(HuaweiFileDownloadManager.this.timeout, currentFileRequest.timeout * 1000L);
            }
        });
        try {
            getFileBlockRequest.doPerform();
        } catch (IOException e) {
            currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileBlockRequest, e));
            reset();
        }
    }

    private void handleFileData(boolean newSync, byte number, byte[] data) {
        if (newSync && currentFileRequest.fileId != number) {
            currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadFileMismatchException(currentFileRequest, number, true));
            reset();
            return;
        }
        if (!newSync) {
            if (currentFileRequest.lastPacketNumber != number - 1) {
                currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadFileMismatchException(currentFileRequest, number, false));
                reset();
                return;
            }
            currentFileRequest.lastPacketNumber = number;
        }

        currentFileRequest.buffer.put(data);

        if (currentFileRequest.buffer.position() >= currentFileRequest.fileSize) {
            // File complete
            LOG.info("Download complete for file {}", currentFileRequest.filename);
            if (currentFileRequest.buffer.position() != currentFileRequest.fileSize) {
                GB.toast("Downloaded file is larger than expected", Toast.LENGTH_SHORT, GB.ERROR);
                LOG.error("Downloaded file is larger than expected: {}", currentFileRequest.filename);
            }
            fileComplete();
        } else if (currentFileRequest.buffer.position() - currentFileRequest.startOfBlockOffset >= currentFileRequest.maxBlockSize) {
            // Block complete, need to request a new file block
            downloadNextFileBlock();
        } // Else we're expecting more data to arrive automatically
    }

    private void fileComplete() {
        // Stop timeout from hitting
        this.handler.removeCallbacks(this.timeout);

        // File complete request
        GetFileDownloadCompleteRequest getFileDownloadCompleteRequest = new GetFileDownloadCompleteRequest(supportProvider, currentFileRequest);
        try {
            getFileDownloadCompleteRequest.doPerform();
        } catch (IOException e) {
            currentFileRequest.fileDownloadCallback.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileDownloadCompleteRequest, e));
            reset();
        }

        // Handle file data
        try {
            currentFileRequest.fileDownloadCallback.downloadComplete(currentFileRequest);
        } catch (Exception e) {
            LOG.error("Download complete callback exception.", e);
            LOG.warn("File contents: {}", GB.hexdump(currentFileRequest.getData()));
            GB.toast("Workout GPX file could not be parsed.",Toast.LENGTH_SHORT, GB.ERROR, e);
        }

        if (!this.currentFileRequest.newSync && !this.fileRequests.isEmpty() && !this.fileRequests.get(0).newSync) {
            // Old sync can potentially take a shortcut
            if (arrayContains(this.currentFileRequest.filenames, this.fileRequests.get(0).filename)) {
                // Shortcut to next download
                // - No init
                // - No getting parameters
                // Just copy over the info and go directly to GetFileInfo
                FileRequest nextRequest = this.fileRequests.remove(0);

                nextRequest.filenames = this.currentFileRequest.filenames;
                nextRequest.maxBlockSize = this.currentFileRequest.maxBlockSize;
                nextRequest.timeout = this.currentFileRequest.timeout;

                this.currentFileRequest = nextRequest;
                getFileInfo();
                return;
            }
        }

        reset();
    }

    private void reset() {
        // Stop listening for file data, if we were doing that
        this.supportProvider.removeInProgressRequests(this.fileDataReceiver);

        // Reset current request
        this.currentFileRequest = null;

        // Unset busy, otherwise the next download will never start
        synchronized (this.supportProvider) {
            this.isBusy = false;
        }
        // Try to download next file
        startDownload();
    }

    public void dispose() {
        // Stop timeout from hitting, nothing else to really do
        this.handler.removeCallbacks(this.timeout);
    }
}

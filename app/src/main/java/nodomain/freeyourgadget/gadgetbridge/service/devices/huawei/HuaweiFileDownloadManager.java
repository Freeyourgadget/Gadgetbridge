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

    /**
     * Only for internal use
     */
    public enum FileType {
        DEBUG,
        SLEEP_STATE,
        SLEEP_DATA,
        UNKNOWN // Never for input!
    }

    /**
     * Only for internal use, though also used in exception
     */
    public static class FileRequest {
        // Inputs

        public String filename;
        public FileType fileType;
        public boolean newSync;

        // Sleep type only
        public int startTime;
        public int endTime;


        // Retrieved

        public int fileSize;
        public int maxBlockSize;
        public int timeout; // TODO: unit?
        public ByteBuffer buffer;

        public int startOfBlockOffset;
        public int currentBlockSize;

        // Old sync only
        public String[] filenames;
        public byte lastPacketNumber;

        // New sync only
        public byte fileId;
        public boolean noEncrypt;
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
                this.newSync = false;
                this.number = ((FileDownloadService0A.BlockResponse) this.receivedPacket).number;
                this.data = ((FileDownloadService0A.BlockResponse) this.receivedPacket).data;
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

    public HuaweiFileDownloadManager(HuaweiSupportProvider supportProvider) {
        this.supportProvider = supportProvider;
        handler = new Handler(Looper.getMainLooper());
        isBusy = false;
        fileRequests = new ArrayList<>();
        timeout = () -> {
            this.supportProvider.downloadException(new HuaweiFileDownloadTimeoutException(currentFileRequest));
            reset();
        };
    }

    public void downloadDebug(String filename) {
        FileRequest request = new FileRequest();
        request.filename = filename;
        request.fileType = FileType.DEBUG;
        request.newSync = false;
        synchronized (supportProvider) {
            fileRequests.add(request);
        }
        startDownload();
    }

    public void downloadSleep(boolean supportsTruSleepNewSync, String filename, int startTime, int endTime) {
        FileRequest request = new FileRequest();
        request.filename = filename;
        request.fileType = (filename.equals("sleep_state.bin"))?FileType.SLEEP_STATE: FileType.SLEEP_DATA;
        request.newSync = supportsTruSleepNewSync;
        request.startTime = startTime;
        request.endTime = endTime;
        synchronized (supportProvider) {
            fileRequests.add(request);
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
                    supportProvider.downloadException(new HuaweiFileDownloadRequestException(null, this.getClass(), e));
                }
            });
        }
    }

    public void startDownload() {
        initFileDataReceiver(); // Make sure the fileDataReceiver is ready

        synchronized (this.supportProvider) {
            if (this.isBusy)
                return; // Already downloading, this file will come eventually
            if (this.fileRequests.isEmpty()) {
                // No more files to download
                supportProvider.downloadQueueEmpty();
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
                        supportProvider.downloadException(new HuaweiFileDownloadFileMismatchException(
                                currentFileRequest,
                                r.filename
                        ));
                        reset();
                        return;
                    }
                    if (currentFileRequest.fileType != r.fileType) {
                        supportProvider.downloadException(new HuaweiFileDownloadFileMismatchException(
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
                    if (!arrayContains(r.filenames, currentFileRequest.filename)) {
                        supportProvider.downloadException(new HuaweiFileDownloadFileMismatchException(
                                currentFileRequest,
                                r.filenames
                        ));
                        reset();
                        return;
                    }
                    currentFileRequest.filenames = r.filenames;
                    getDownloadParameters();
                }
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                supportProvider.downloadException(new HuaweiFileDownloadRequestException(currentFileRequest, this.getClass(), e));
            }
        });
        try {
            getFileDownloadInitRequest.doPerform();
        } catch (IOException e) {
            supportProvider.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileDownloadInitRequest, e));
            reset();
        }
    }

    private void getDownloadParameters() {
        // Old sync only, can never be multiple at the same time
        // Assuming currentRequest is the correct one the entire time
        // Which may no longer be the case when we implement multi-download for new sync
        GetFileParametersRequest getFileParametersRequest = new GetFileParametersRequest(supportProvider);
        getFileParametersRequest.setFinalizeReq(new Request.RequestCallback() {
            @Override
            public void call() {
                currentFileRequest.maxBlockSize = getFileParametersRequest.getMaxBlockSize();
                currentFileRequest.timeout = getFileParametersRequest.getTimeout();
                getFileInfo();
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                supportProvider.downloadException(new HuaweiFileDownloadRequestException(null, this.getClass(), e));
                reset();
            }
        });
        try {
            getFileParametersRequest.doPerform();
        } catch (IOException e) {
            supportProvider.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileParametersRequest, e));
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
                        supportProvider.downloadException(new HuaweiFileDownloadFileMismatchException(currentFileRequest, r.fileId, true));
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
                supportProvider.downloadException(new HuaweiFileDownloadRequestException(null, this.getClass(), e));
                reset();
            }
        });
        try {
            getFileInfoRequest.doPerform();
        } catch (IOException e) {
            supportProvider.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileInfoRequest, e));
            reset();
        }
    }

    private void downloadNextFileBlock() {
        handler.removeCallbacks(this.timeout);

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
            supportProvider.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileBlockRequest, e));
            reset();
        }
    }

    private void handleFileData(boolean newSync, byte number, byte[] data) {
        if (newSync && currentFileRequest.fileId != number) {
            supportProvider.downloadException(new HuaweiFileDownloadFileMismatchException(currentFileRequest, number, true));
            reset();
            return;
        }
        if (!newSync) {
            if (currentFileRequest.lastPacketNumber != number - 1) {
                supportProvider.downloadException(new HuaweiFileDownloadFileMismatchException(currentFileRequest, number, false));
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
            supportProvider.downloadException(new HuaweiFileDownloadSendException(currentFileRequest, getFileDownloadCompleteRequest, e));
            reset();
        }

        // Handle file data
        if (currentFileRequest.buffer != null) // File size was zero
            supportProvider.downloadComplete(currentFileRequest.filename, currentFileRequest.buffer.array());

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

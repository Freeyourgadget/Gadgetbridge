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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileDownloadService0A;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileDownloadService2C;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiFileDownloadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetFileDownloadInitRequest extends Request {

    final HuaweiFileDownloadManager.FileRequest request;

    public boolean newSync;

    // Old sync
    public String[] filenames;

    // New sync
    public String filename;
    public HuaweiFileDownloadManager.FileType fileType;
    public byte fileId;
    public int fileSize;

    public GetFileDownloadInitRequest(HuaweiSupportProvider support, HuaweiFileDownloadManager.FileRequest request) {
        super(support);
        if (request.newSync) {
            this.serviceId = FileDownloadService2C.id;
            this.commandId = FileDownloadService2C.FileDownloadInit.id;
        } else {
            this.serviceId = FileDownloadService0A.id;
            this.commandId = FileDownloadService0A.FileDownloadInit.id;
        }
        this.request = request;
    }

    private FileDownloadService2C.FileType convertFileTypeTo2C(HuaweiFileDownloadManager.FileType type) {
        switch (type) {
            case SLEEP_STATE:
                return FileDownloadService2C.FileType.SLEEP_STATE;
            case SLEEP_DATA:
                return FileDownloadService2C.FileType.SLEEP_DATA;
            default:
                return FileDownloadService2C.FileType.UNKNOWN;
        }
    }

    private HuaweiFileDownloadManager.FileType convertFileTypeFrom2C(FileDownloadService2C.FileType type) {
        switch (type) {
            case SLEEP_STATE:
                return HuaweiFileDownloadManager.FileType.SLEEP_STATE;
            case SLEEP_DATA:
                return HuaweiFileDownloadManager.FileType.SLEEP_DATA;
            default:
                return HuaweiFileDownloadManager.FileType.UNKNOWN;
        }
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            if (this.request.newSync) {
                FileDownloadService2C.FileType type = convertFileTypeTo2C(request.fileType);
                if (type == FileDownloadService2C.FileType.UNKNOWN)
                    throw new RequestCreationException("Cannot convert type " + request.fileType);
                return new FileDownloadService2C.FileDownloadInit.Request(paramsProvider, request.filename, type, request.startTime, request.endTime).serialize();
            } else {
                if (this.request.fileType == HuaweiFileDownloadManager.FileType.DEBUG)
                    return new FileDownloadService0A.FileDownloadInit.DebugFilesRequest(paramsProvider).serialize();
                else if (this.request.fileType == HuaweiFileDownloadManager.FileType.SLEEP_STATE)
                    return new FileDownloadService0A.FileDownloadInit.SleepFilesRequest(paramsProvider, request.startTime, request.endTime).serialize();
                else
                    throw new RequestCreationException("Unknown file type");
            }
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        // In case of multiple downloads, the response here does not need to match the original request
        // So we cannot use the request at all!

        if (this.receivedPacket instanceof FileDownloadService0A.FileDownloadInit.Response) {
            this.newSync = false;
            this.filenames = ((FileDownloadService0A.FileDownloadInit.Response) this.receivedPacket).fileNames;
        } else if (this.receivedPacket instanceof FileDownloadService2C.FileDownloadInit.Response) {
            this.newSync = true;
            FileDownloadService2C.FileDownloadInit.Response packet = (FileDownloadService2C.FileDownloadInit.Response) this.receivedPacket;
            this.filename = packet.fileName;
            this.fileType = convertFileTypeFrom2C(packet.fileType);
            this.fileId = packet.fileId;
            this.fileSize = packet.fileSize;
        } else {
            throw new ResponseTypeMismatchException(receivedPacket, FileDownloadService2C.FileDownloadInit.Response.class);
        }
    }
}

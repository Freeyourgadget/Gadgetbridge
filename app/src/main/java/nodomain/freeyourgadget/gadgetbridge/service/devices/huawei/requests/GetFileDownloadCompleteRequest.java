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

public class GetFileDownloadCompleteRequest extends Request {

    private final HuaweiFileDownloadManager.FileRequest request;

    public GetFileDownloadCompleteRequest(HuaweiSupportProvider support, HuaweiFileDownloadManager.FileRequest request) {
        super(support);
        if (request.isNewSync()) {
            this.serviceId = FileDownloadService2C.id;
            this.commandId = FileDownloadService2C.FileDownloadCompleteRequest.id;
        } else {
            this.serviceId = FileDownloadService0A.id;
            this.commandId = FileDownloadService0A.FileDownloadCompleteRequest.id;
        }
        this.request = request;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            if (request.isNewSync())
                return new FileDownloadService2C.FileDownloadCompleteRequest(paramsProvider, this.request.getFileId()).serialize();
            else
                return new FileDownloadService0A.FileDownloadCompleteRequest(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}

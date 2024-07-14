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
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetFileParametersRequest extends Request {

    private int maxBlockSize;
    private int timeout;

    public GetFileParametersRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FileDownloadService0A.id;
        this.commandId = FileDownloadService0A.FileParameters.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new FileDownloadService0A.FileParameters.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(this.receivedPacket instanceof FileDownloadService0A.FileParameters.Response))
            throw new ResponseTypeMismatchException(this.receivedPacket, FileDownloadService0A.FileParameters.Response.class);
        this.maxBlockSize = ((FileDownloadService0A.FileParameters.Response) this.receivedPacket).maxBlockSize;
        this.timeout = ((FileDownloadService0A.FileParameters.Response) this.receivedPacket).timeout;
    }

    public int getMaxBlockSize() {
        return maxBlockSize;
    }

    public int getTimeout() {
        return timeout;
    }
}

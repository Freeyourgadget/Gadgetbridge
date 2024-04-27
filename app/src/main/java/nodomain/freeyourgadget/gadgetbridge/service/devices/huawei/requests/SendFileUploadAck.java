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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendFileUploadAck extends Request {
    byte noEncryption = 0;
    byte fileType = 1;
    public SendFileUploadAck(HuaweiSupportProvider support, byte noEncryption, byte fileType) {
        super(support);
        this.serviceId = FileUpload.id;
        this.commandId = FileUpload.FileUploadConsultAck.id;
        this.noEncryption = noEncryption;
        this.fileType = fileType;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new FileUpload.FileUploadConsultAck.Request(this.paramsProvider, this.noEncryption, this.fileType).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}

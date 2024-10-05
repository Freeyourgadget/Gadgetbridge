package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.EphemerisFileUpload;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendEphemerisSingleFileInfoResponse extends Request {
    final int responseCode;
    final int fileSize;
    final short crc;

    public SendEphemerisSingleFileInfoResponse(HuaweiSupportProvider support, int responseCode, int fileSize, short crc) {
        super(support);
        this.serviceId = EphemerisFileUpload.id;
        this.commandId = EphemerisFileUpload.FileList.id;
        this.responseCode = responseCode;
        this.fileSize = fileSize;
        this.crc = crc;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new EphemerisFileUpload.QuerySingleFileInfo.QuerySingleFileInfoResponse(this.paramsProvider, this.responseCode, this.fileSize, this.crc).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }
}

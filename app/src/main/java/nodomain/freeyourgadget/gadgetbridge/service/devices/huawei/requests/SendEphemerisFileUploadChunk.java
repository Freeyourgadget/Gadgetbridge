package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.EphemerisFileUpload;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;


public class SendEphemerisFileUploadChunk extends Request {
    byte[] fileChunk;
    short transferSize;
    int packetCount;
    public SendEphemerisFileUploadChunk(HuaweiSupportProvider support, byte[] fileChunk, short transferSize, int packetCount) {
        super(support);
        this.serviceId = EphemerisFileUpload.id;
        this.commandId = EphemerisFileUpload.UploadData.id;
        this.fileChunk = fileChunk;
        this.transferSize = transferSize;
        this.packetCount = packetCount;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new EphemerisFileUpload.UploadData.FileNextChunkSend(this.paramsProvider).serializeFileChunk1c(
                    fileChunk,
                    transferSize,
                    packetCount
            );
        } catch(HuaweiPacket.SerializeException e) {
            throw new RequestCreationException(e.getMessage());
        }
    }
}
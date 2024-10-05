package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Ephemeris;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendEphemerisFileStatusRequest extends Request  {
    private final Logger LOG = LoggerFactory.getLogger(SendEphemerisFileStatusRequest.class);

    private final byte status;
    public SendEphemerisFileStatusRequest(HuaweiSupportProvider support, byte status) {
        super(support);
        this.serviceId = Ephemeris.id;
        this.commandId = Ephemeris.FileStatus.id;
        this.status = status;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new Ephemeris.FileStatus.Request(this.paramsProvider, status).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Ephemeris.FileStatus.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Ephemeris.FileStatus.Response.class);
        LOG.info("Ephemeris FileStatus Response code: {}", ((Ephemeris.FileStatus.Response) receivedPacket).responseCode);
    }
}

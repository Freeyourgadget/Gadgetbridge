package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetExtendedMusicInfoParams extends Request {
    private final Logger LOG = LoggerFactory.getLogger(GetExtendedMusicInfoParams.class);
    public GetExtendedMusicInfoParams(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.ExtendedMusicInfoParams.id;

    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsMusicUploading();
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.ExtendedMusicInfoParams.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws Request.ResponseParseException {
        LOG.info("MusicControl.ExtendedMusicInfoParams processResponse");
        if (!(receivedPacket instanceof MusicControl.ExtendedMusicInfoParams.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, MusicControl.ExtendedMusicInfoParams.Response.class);

        MusicControl.ExtendedMusicInfoParams.Response resp = (MusicControl.ExtendedMusicInfoParams.Response)(receivedPacket);
        supportProvider.getHuaweiCoordinator().setExtendedMusicInfoParams(resp.params);
    }
}

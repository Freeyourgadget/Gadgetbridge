package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetMusicInfoParams extends Request {
    private final Logger LOG = LoggerFactory.getLogger(GetMusicInfoParams.class);
    public GetMusicInfoParams(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.MusicInfoParams.id;

    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsMusicUploading();
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.MusicInfoParams.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws Request.ResponseParseException {
        LOG.info("MusicControl.MusicInfoParams processResponse");
        if (!(receivedPacket instanceof MusicControl.MusicInfoParams.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, MusicControl.MusicInfoParams.Response.class);

        MusicControl.MusicInfoParams.Response resp = (MusicControl.MusicInfoParams.Response)(receivedPacket);
        supportProvider.getHuaweiMusicManager().onMusicMusicInfoParams(resp.params, resp.frameCount, resp.pageStruct);
    }
}

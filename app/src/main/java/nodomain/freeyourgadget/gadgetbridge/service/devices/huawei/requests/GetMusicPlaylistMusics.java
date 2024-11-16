package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetMusicPlaylistMusics extends Request {
    private final Logger LOG = LoggerFactory.getLogger(GetMusicPlaylistMusics.class);

    private final int playlist;
    private final int index;

    public GetMusicPlaylistMusics(HuaweiSupportProvider support, int playlist, int index) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.MusicPlaylistMusics.id;
        this.playlist = playlist;
        this.index = index;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.MusicPlaylistMusics.Request(paramsProvider, (short) playlist, (short) index).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws Request.ResponseParseException {
        LOG.info("MusicControl.GetMusicPlaylistMusics processResponse");
        if (!(receivedPacket instanceof MusicControl.MusicPlaylistMusics.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, MusicControl.MusicPlaylistMusics.Response.class);

        MusicControl.MusicPlaylistMusics.Response resp = (MusicControl.MusicPlaylistMusics.Response) (receivedPacket);
        supportProvider.getHuaweiMusicManager().onMusicPlaylistMusics(resp.id, resp.index, resp.musicIds);
    }
}

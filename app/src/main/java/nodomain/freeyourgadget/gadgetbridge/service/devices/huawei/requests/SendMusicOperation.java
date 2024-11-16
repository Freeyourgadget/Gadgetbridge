package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendMusicOperation extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendMusicOperation.class);

    private final int operation;
    private final int playlistIndex;
    private final String playlistName;
    private final ArrayList<Integer> musicIds;


    public SendMusicOperation(HuaweiSupportProvider support, int operation, int playlistIndex, String playlistName, ArrayList<Integer> musicIds) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.MusicOperation.id;
        this.operation = operation;
        this.playlistIndex = playlistIndex;
        this.playlistName = playlistName;
        this.musicIds = musicIds;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.MusicOperation.Request(paramsProvider, operation, playlistIndex, playlistName, musicIds).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseTypeMismatchException {
        LOG.debug("handle Music Operation");
        if (!(receivedPacket instanceof MusicControl.MusicOperation.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, MusicControl.MusicOperation.Response.class);

        MusicControl.MusicOperation.Response resp = (MusicControl.MusicOperation.Response) (receivedPacket);
        supportProvider.getHuaweiMusicManager().onMusicOperationResponse(resp.resultCode, resp.operation, resp.playlistIndex, resp.playlistName, resp.musicIds);
    }

}

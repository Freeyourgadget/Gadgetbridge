package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendUploadMusicFileInfoResponse extends Request {
    short songIndex;
    String songName;
    String songArtist;


    public SendUploadMusicFileInfoResponse(HuaweiSupportProvider support, short songIndex, String songName, String songArtist) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.UploadMusicFileInfo.id;
        this.songIndex = songIndex;
        this.songName = songName;
        this.songArtist = songArtist;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.UploadMusicFileInfo.UploadMusicFileInfoResponse(this.paramsProvider,  this.songIndex, this.songName, this.songArtist).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }
}
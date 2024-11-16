package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusic;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusicPlaylist;

public class GBDeviceMusicData extends GBDeviceEvent {
    public int type = 0; // 1 - sync start, 2 - music list, 10 - end sync
    public List<GBDeviceMusic> list = null;
    public List<GBDeviceMusicPlaylist> playlists = null;
    public String deviceInfo = null;
    public int maxMusicCount = 0;
    public int maxPlaylistCount = 0;
}

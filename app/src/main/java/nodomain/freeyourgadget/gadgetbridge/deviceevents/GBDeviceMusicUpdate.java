package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import java.util.ArrayList;

public class GBDeviceMusicUpdate extends GBDeviceEvent {
    public boolean success = false;
    public int operation = -1;
    public int playlistIndex = -1;
    public String playlistName;
    public ArrayList<Integer> musicIds = null;
}

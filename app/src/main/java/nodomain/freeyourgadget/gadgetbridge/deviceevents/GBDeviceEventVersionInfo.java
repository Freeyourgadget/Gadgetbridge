package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class GBDeviceEventVersionInfo extends GBDeviceEvent {
    public String fwVersion = GBApplication.getContext().getString(R.string.n_a);
    public String hwVersion = GBApplication.getContext().getString(R.string.n_a);
}

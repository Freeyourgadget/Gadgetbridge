package nodomain.freeyourgadget.gadgetbridge.protocol;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class GBDeviceCommandVersionInfo extends GBDeviceCommand {
    public String fwVersion = GBApplication.getContext().getString(R.string.n_a);
    public String hwVersion = GBApplication.getContext().getString(R.string.n_a);

    public GBDeviceCommandVersionInfo() {
        commandClass = CommandClass.VERSION_INFO;
    }
}

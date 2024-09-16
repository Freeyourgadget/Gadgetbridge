package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminForerunner245MusicCoordinator extends GarminCoordinator {
    @Override
    public boolean isExperimental() {
        // https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/3986
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Forerunner 245 Music$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_forerunner_245_music;
    }
}

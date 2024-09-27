package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminForerunner245Coordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Forerunner 245$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_forerunner_245;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        return 10;
    }

    @Override
    public boolean supportsAlarmSounds(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAlarmBacklight(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAlarmTitlePresets(final GBDevice device) {
        return true;
    }

    @Override
    public List<String> getAlarmTitlePresets(final GBDevice device) {
        return Arrays.asList(
                "label 1",
                "label 2",
                "label 3"
        );
    }
}

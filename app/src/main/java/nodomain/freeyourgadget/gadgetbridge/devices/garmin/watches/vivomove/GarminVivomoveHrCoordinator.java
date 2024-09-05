package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove;

import android.app.Activity;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.activities.calibration.HandCalibrationActivity;

public class GarminVivomoveHrCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívomove HR$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivomove_hr;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getCalibrationActivity() {
        return HandCalibrationActivity.class;
    }
}

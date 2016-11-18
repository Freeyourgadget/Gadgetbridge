package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class MiBand2Coordinator extends MiBandCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand2Coordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND2;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        // and a heuristic
        try {
            BluetoothDevice device = candidate.getDevice();
            if (isHealthWearable(device)) {
                String name = device.getName();
                return name != null && name.equalsIgnoreCase(MiBandConst.MI_BAND2_NAME);
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return false;

    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAlarmConfiguration() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null; // not supported at the moment
    }

    public static DateTimeDisplay getDateDisplay(Context context) throws IllegalArgumentException {
        Prefs prefs = GBApplication.getPrefs();
        String dateFormatTime = context.getString(R.string.p_dateformat_time);
        if (dateFormatTime.equals(prefs.getString(MiBandConst.PREF_MI2_DATEFORMAT, dateFormatTime))) {
            return DateTimeDisplay.TIME;
        }
        return DateTimeDisplay.DATE_TIME;
    }

    public static boolean getActivateDisplayOnLiftWrist() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MI2_ACTIVATE_DISPLAY_ON_LIFT, true);
    }
}

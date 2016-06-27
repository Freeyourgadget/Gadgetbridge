package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class PebbleCoordinator extends AbstractDeviceCoordinator {
    public PebbleCoordinator() {
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return candidate.getName().startsWith("Pebble");
    }

    @Override
    public boolean supports(GBDevice device) {
        return getDeviceType().equals(device.getType());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.PEBBLE;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    public Class<? extends Activity> getPrimaryActivity() {
        return AppManagerActivity.class;
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        Prefs prefs = GBApplication.getPrefs();
        int activityTracker = prefs.getInt("pebble_activitytracker", SampleProvider.PROVIDER_PEBBLE_HEALTH);
        switch (activityTracker) {
            case SampleProvider.PROVIDER_PEBBLE_HEALTH:
                return new HealthSampleProvider(device, session);
            case SampleProvider.PROVIDER_PEBBLE_MISFIT:
                return new MisfitSampleProvider(device, session);
            case SampleProvider.PROVIDER_PEBBLE_MORPHEUZ:
                return new MorpheuzSampleProvider(device, session);
            case SampleProvider.PROVIDER_PEBBLE_GADGETBRIDGE:
                return new PebbleGadgetBridgeSampleProvider(device, session);
            default:
                return new HealthSampleProvider(device, session);
        }
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        PBWInstallHandler installHandler = new PBWInstallHandler(uri, context);
        return installHandler.isValid() ? installHandler : null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsScreenshots() {
        return true;
    }

    @Override
    public boolean supportsAlarmConfiguration() {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false;
    }

    @Override
    public int getTapString() {
        return R.string.tap_connected_device_for_app_mananger;
    }

    @Override
    public String getManufacturer() {
        return "Pebble";
    }
}

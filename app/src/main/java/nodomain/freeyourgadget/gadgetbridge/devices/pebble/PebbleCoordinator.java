package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class PebbleCoordinator implements DeviceCoordinator {
    private MorpheuzSampleProvider sampleProvider;

    public PebbleCoordinator() {
        // FIXME: make this configurable somewhere else
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        if (sharedPrefs.getBoolean("pebble_force_untested", false)) {
            sampleProvider = new PebbleGadgetBridgeSampleProvider();
        } else {
            sampleProvider = new MorpheuzSampleProvider();
        }
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
    public SampleProvider getSampleProvider() {
        return sampleProvider;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        PBWInstallHandler installHandler = new PBWInstallHandler(uri, context);
        return installHandler.isValid() ? installHandler : null;
    }
}

package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.app.Activity;

import nodomain.freeyourgadget.gadgetbridge.activities.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public class PebbleCoordinator implements DeviceCoordinator {
    private MorpheuzSampleProvider sampleProvider;

    public PebbleCoordinator() {
        sampleProvider = new MorpheuzSampleProvider();
//        sampleProvider = new PebbleGadgetBridgeSampleProvider();
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
}

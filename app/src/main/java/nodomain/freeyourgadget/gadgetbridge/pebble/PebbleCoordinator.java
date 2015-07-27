package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.app.Activity;

import nodomain.freeyourgadget.gadgetbridge.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.discovery.DeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.SampleProvider;

public class PebbleCoordinator implements DeviceCoordinator {
    private MorpheuzSampleProvider sampleProvider;

    public PebbleCoordinator() {
        sampleProvider = new MorpheuzSampleProvider();
//        sampleProvider = new PebbleGadgetBridgeSampleProvider();
    }

    @Override
    public boolean supports(DeviceCandidate candidate) {
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

package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;

import nodomain.freeyourgadget.gadgetbridge.charts.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.discovery.DeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.SampleProvider;

public class UnknownDeviceCoordinator implements DeviceCoordinator {
    private final UnknownSampleProvider sampleProvider;

    private static final class UnknownSampleProvider implements SampleProvider {
        @Override
        public int normalizeType(byte rawType) {
            return ActivityKind.TYPE_UNKNOWN;
        }

        @Override
        public byte toRawActivityKind(int activityKind) {
            return 0;
        }

        @Override
        public float normalizeIntensity(short rawIntensity) {
            return 0;
        }

        @Override
        public byte getID() {
            return SampleProvider.PROVIDER_UNKNOWN;
        }
    }

    public UnknownDeviceCoordinator() {
        sampleProvider = new UnknownSampleProvider();
    }

    @Override
    public boolean supports(DeviceCandidate candidate) {
        return false;
    }

    @Override
    public boolean supports(GBDevice device) {
        return getDeviceType().equals(device.getType());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.UNKNOWN;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return ControlCenter.class;
    }

    @Override
    public Class<? extends Activity> getPrimaryActivity() {
        return null;
    }

    @Override
    public SampleProvider getSampleProvider() {
        return sampleProvider;
    }
}

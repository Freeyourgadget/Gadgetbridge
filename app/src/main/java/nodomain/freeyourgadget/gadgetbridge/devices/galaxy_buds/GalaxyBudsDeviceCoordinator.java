package nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class GalaxyBudsDeviceCoordinator extends AbstractDeviceCoordinator {

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {

        String name = candidate.getName();

        if (name != null && (
                name.startsWith("Galaxy Buds (")
        )) {
            return DeviceType.GALAXY_BUDS;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.GALAXY_BUDS;
    }

    @Override
    public int getBatteryCount() {
        return 2;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_galaxy_buds_l, R.string.left_earbud);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2};
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_BOND;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return false;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice
                                                                              device, DaoSession session) {
        return null;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public int getAlarmSlotCount() {
        return 0;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false;
    }

    @Override
    public String getManufacturer() {
        return "Samsung";
    }

    @Override
    public boolean supportsAppsManagement() {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return false;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device
            device, @NonNull DaoSession session) throws GBException {

    }


    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_galaxy_buds,
        };
    }
}

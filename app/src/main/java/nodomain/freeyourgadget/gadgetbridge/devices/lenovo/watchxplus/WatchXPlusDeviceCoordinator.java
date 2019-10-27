package nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.LenovoWatchPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class WatchXPlusDeviceCoordinator extends AbstractDeviceCoordinator {


    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid watchXpService = new ParcelUuid(WatchXPlusConstants.UUID_SERVICE_WATCHXPLUS);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(watchXpService).build();
        return Collections.singletonList(filter);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String macAddress = candidate.getMacAddress().toUpperCase();
        String deviceName = candidate.getName().toUpperCase();
        if (candidate.supportsService(WatchXPlusConstants.UUID_SERVICE_WATCHXPLUS)) {
            return DeviceType.WATCHXPLUS;
        } else if (macAddress.startsWith("DC:41:E5")) {
            return DeviceType.WATCHXPLUS;
        } else if (deviceName.equalsIgnoreCase("WATCH XPLUS")) {
            return DeviceType.WATCHXPLUS;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.WATCHXPLUS;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return LenovoWatchPairingActivity.class;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new WatchXPlusSampleProvider(device, session);
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
        return 3;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return "Lenovo";
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
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }
}

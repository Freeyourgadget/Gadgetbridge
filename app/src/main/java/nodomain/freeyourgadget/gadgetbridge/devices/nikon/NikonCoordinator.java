package nodomain.freeyourgadget.gadgetbridge.devices.nikon;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class NikonCoordinator extends AbstractDeviceCoordinator {
    // @NonNull
    // @Override
    // @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    // public Collection<? extends ScanFilter> createBLEScanFilters() {
    //     ParcelUuid service = new ParcelUuid(ID115Constants.UUID_SERVICE_ID115);
    //     ScanFilter filter = new ScanFilter.Builder().setServiceUuid(service).build();
    //     return Collections.singletonList(filter);
    // }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        //if (candidate.supportsService(ID115Constants.UUID_SERVICE_ID115)) {
        //    return DeviceType.NIKON;
        //}

        String name = candidate.getDevice().getName();
        if(name != null && name.startsWith("D3400")){
            return DeviceType.NIKON;
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public int getBondingStyle(GBDevice deviceCandidate){
        return BONDING_STYLE_BOND;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.NIKON;
    }

    @Override
    public String getManufacturer() {
        return "Nikon";
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
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
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
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
    public boolean supportsAlarmConfiguration() {
        return false;
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
        return false;
    }
}

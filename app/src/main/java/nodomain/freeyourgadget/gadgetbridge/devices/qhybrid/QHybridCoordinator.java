package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class QHybridCoordinator extends AbstractDeviceCoordinator {

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        for(ParcelUuid uuid : candidate.getServiceUuids()){
            if(uuid.getUuid().toString().equals("00001812-0000-1000-8000-00805f9b34fb")){
                return DeviceType.FOSSILQHYBRID;
            }
        }
        return DeviceType.UNKNOWN;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        return Collections.singletonList(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb")).build());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.FOSSILQHYBRID;
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

    public boolean supportsAlarmConfiguration() {
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
        return "Fossil";
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
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }


}

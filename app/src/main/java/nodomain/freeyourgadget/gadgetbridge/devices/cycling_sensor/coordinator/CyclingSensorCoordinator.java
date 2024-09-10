package nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.coordinator;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.activity.CyclingLiveDataActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.db.CyclingSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling_sensor.support.CyclingSensorSupport;

public class CyclingSensorCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        final Long deviceId = device.getId();

        session.getCyclingSampleDao().queryBuilder()
                .where(CyclingSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return candidate.supportsService(CyclingSensorSupport.UUID_CYCLING_SENSOR_SERVICE);
    }

    @Override
    public boolean supportsCyclingData() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public TimeSampleProvider<CyclingSample> getCyclingSampleProvider(GBDevice device, DaoSession session) {
        return new CyclingSampleProvider(device, session);
    }

    @Override
    public boolean supportsSleepMeasurement() {
        return false;
    }
    @Override
    public boolean supportsStepCounter() {
        return false;
    }
    @Override
    public boolean supportsSpeedzones() {
        return false;
    }
    @Override
    public boolean supportsActivityTabs() {
        return false;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public String getManufacturer() {
        return "Generic";
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return CyclingLiveDataActivity.class;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_cycling_sensor
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return CyclingSensorSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_cycling_sensor;
    }

    @Override
    public boolean supportsAppsManagement(GBDevice device) {
        return true;
    }


}

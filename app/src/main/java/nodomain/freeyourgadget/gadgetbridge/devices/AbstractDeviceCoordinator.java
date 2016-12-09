package nodomain.freeyourgadget.gadgetbridge.devices;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributesDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;

public abstract class AbstractDeviceCoordinator implements DeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceCoordinator.class);

    @Override
    public final boolean supports(GBDeviceCandidate candidate) {
        return getSupportedType(candidate).isSupported();
    }

    @Override
    public boolean supports(GBDevice device) {
        return getDeviceType().equals(device.getType());
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        return Collections.emptyList();
    }

    @Override
    public GBDevice createDevice(GBDeviceCandidate candidate) {
        return new GBDevice(candidate.getDevice().getAddress(), candidate.getName(), getDeviceType());
    }

    @Override
    public void deleteDevice(final GBDevice gbDevice) throws GBException {
        LOG.info("will try to delete device: " + gbDevice.getName());
        if (gbDevice.isConnected() || gbDevice.isConnecting()) {
            GBApplication.deviceService().disconnect();
        }
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Device device = DBHelper.findDevice(gbDevice, session);
            if (device != null) {
                deleteDevice(gbDevice, device, session);
                QueryBuilder<?> qb = session.getDeviceAttributesDao().queryBuilder();
                qb.where(DeviceAttributesDao.Properties.DeviceId.eq(device.getId())).buildDelete().executeDeleteWithoutDetachingEntities();
                session.getDeviceDao().delete(device);
            } else {
                LOG.info("device to delete not found in db: " + gbDevice);
            }
        } catch (Exception e) {
            throw new GBException("Error deleting device: " + e.getMessage(), e);
        }
    }

    /**
     * Hook for subclasses to perform device-specific deletion logic, e.g. db cleanup.
     * @param gbDevice the GBDevice
     * @param device the corresponding database Device
     * @param session the session to use
     * @throws GBException
     */
    protected abstract void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException;

    @Override
    public boolean allowFetchActivityData(GBDevice device) {
        return device.isInitialized() && !device.isBusy() && supportsActivityDataFetching();
    }

    public boolean isHealthWearable(BluetoothDevice device) {
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass == null) {
            LOG.warn("unable to determine bluetooth device class of " + device);
            return false;
        }
        if (bluetoothClass.getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE
            || bluetoothClass.getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED) {
            int deviceClasses =
                    BluetoothClass.Device.HEALTH_BLOOD_PRESSURE
                    | BluetoothClass.Device.HEALTH_DATA_DISPLAY
                    | BluetoothClass.Device.HEALTH_PULSE_RATE
                    | BluetoothClass.Device.HEALTH_WEIGHING
                    | BluetoothClass.Device.HEALTH_UNCATEGORIZED
                    | BluetoothClass.Device.HEALTH_PULSE_OXIMETER
                    | BluetoothClass.Device.HEALTH_GLUCOSE;

            return (bluetoothClass.getDeviceClass() & deviceClasses) != 0;
        }
        return false;
    }
}

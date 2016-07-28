package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.CheckInitializedAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;

/**
 * Abstract base class for all devices connected through Bluetooth Low Energy (LE) aka
 * Bluetooth Smart.
 * <p/>
 * The connection to the device and all communication is made with a generic {@link BtLEQueue}.
 * Messages to the device are encoded as {@link BtLEAction actions} that are grouped with a
 * {@link Transaction} and sent via {@link BtLEQueue}.
 *
 * @see TransactionBuilder
 * @see BtLEQueue
 */
public abstract class AbstractBTLEDeviceSupport extends AbstractDeviceSupport implements GattCallback {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBTLEDeviceSupport.class);

    private BtLEQueue mQueue;
    private HashMap<UUID, BluetoothGattCharacteristic> mAvailableCharacteristics;
    private final Set<UUID> mSupportedServices = new HashSet<>(4);
    private final List<AbstractBleProfile<?>> mSupportedProfiles = new ArrayList<>();

    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb"; //this is common for all BTLE devices. see http://stackoverflow.com/questions/18699251/finding-out-android-bluetooth-le-gatt-profiles

    @Override
    public boolean connect() {
        if (mQueue == null) {
            mQueue = new BtLEQueue(getBluetoothAdapter(), getDevice(), this, getContext());
            mQueue.setAutoReconnect(getAutoReconnect());
        }
        return mQueue.connect();
    }

    @Override
    public void setAutoReconnect(boolean enable) {
        super.setAutoReconnect(enable);
        if (mQueue != null) {
            mQueue.setAutoReconnect(enable);
        }
    }

    /**
     * Subclasses should populate the given builder to initialize the device (if necessary).
     *
     * @param builder
     * @return the same builder as passed as the argument
     */
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        return builder;
    }

    @Override
    public void dispose() {
        if (mQueue != null) {
            mQueue.dispose();
            mQueue = null;
        }
    }

    protected TransactionBuilder createTransactionBuilder(String taskName) {
        return new TransactionBuilder(taskName);
    }

    /**
     * Send commands like this to the device:
     * <p>
     * <code>perform("sms notification").write(someCharacteristic, someByteArray).queue(getQueue());</code>
     * </p>
     * TODO: support orchestration of multiple reads and writes depending on returned values
     *
     * @see #performConnected(Transaction)
     * @see #initializeDevice(TransactionBuilder)
     */
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        if (!isConnected()) {
            if (!connect()) {
                throw new IOException("1: Unable to connect to device: " + getDevice());
            }
        }
        if (!isInitialized()) {
            // first, add a transaction that performs device initialization
            TransactionBuilder builder = createTransactionBuilder("Initialize device");
            builder.add(new CheckInitializedAction(gbDevice));
            initializeDevice(builder).queue(getQueue());
        }
        return createTransactionBuilder(taskName);
    }

    /**
     * @param transaction
     * @throws IOException
     * @see {@link #performInitialized(String)}
     */
    protected void performConnected(Transaction transaction) throws IOException {
        if (!isConnected()) {
            if (!connect()) {
                throw new IOException("2: Unable to connect to device: " + getDevice());
            }
        }
        getQueue().add(transaction);
    }

    public BtLEQueue getQueue() {
        return mQueue;
    }

    /**
     * Subclasses should call this method to add services they support.
     * Only supported services will be queried for characteristics.
     *
     * @param aSupportedService
     * @see #getCharacteristic(UUID)
     */
    protected void addSupportedService(UUID aSupportedService) {
        mSupportedServices.add(aSupportedService);
    }

    protected void addSupportedProfile(AbstractBleProfile<?> profile) {
        mSupportedProfiles.add(profile);
    }

    /**
     * Returns the characteristic matching the given UUID. Only characteristics
     * are returned whose service is marked as supported.
     *
     * @param uuid
     * @return the characteristic for the given UUID or <code>null</code>
     * @see #addSupportedService(UUID)
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        if (mAvailableCharacteristics == null) {
            return null;
        }
        return mAvailableCharacteristics.get(uuid);
    }

    private void gattServicesDiscovered(List<BluetoothGattService> discoveredGattServices) {
        if (discoveredGattServices == null) {
            LOG.warn("No gatt services discovered: null!");
            return;
        }
        Set<UUID> supportedServices = getSupportedServices();
        mAvailableCharacteristics = new HashMap<>();
        for (BluetoothGattService service : discoveredGattServices) {
            if (supportedServices.contains(service.getUuid())) {
                LOG.debug("discovered supported service: " + BleNamesResolver.resolveServiceName(service.getUuid().toString()) + ": " + service.getUuid());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                if (characteristics == null || characteristics.isEmpty()) {
                    LOG.warn("Supported LE service " + service.getUuid() + "did not return any characteristics");
                    continue;
                }
                HashMap<UUID, BluetoothGattCharacteristic> intmAvailableCharacteristics = new HashMap<>(characteristics.size());
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    intmAvailableCharacteristics.put(characteristic.getUuid(), characteristic);
                    LOG.info("    characteristic: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()) + ": " + characteristic.getUuid());
                }
                mAvailableCharacteristics.putAll(intmAvailableCharacteristics);
            } else {
                LOG.debug("discovered unsupported service: " + BleNamesResolver.resolveServiceName(service.getUuid().toString()) + ": " + service.getUuid());
            }
        }
    }

    protected Set<UUID> getSupportedServices() {
        return mSupportedServices;
    }

    // default implementations of event handler methods (gatt callbacks)
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        for (AbstractBleProfile profile : mSupportedProfiles) {
            profile.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        gattServicesDiscovered(gatt.getServices());
        initializeDevice(createTransactionBuilder("Initializing device")).queue(getQueue());
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {
        for (AbstractBleProfile profile : mSupportedProfiles) {
            if (profile.onCharacteristicRead(gatt, characteristic, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
        for (AbstractBleProfile profile : mSupportedProfiles) {
            if (profile.onCharacteristicWrite(gatt, characteristic, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        for (AbstractBleProfile profile : mSupportedProfiles) {
            if (profile.onDescriptorRead(gatt, descriptor, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        for (AbstractBleProfile profile : mSupportedProfiles) {
            if (profile.onDescriptorWrite(gatt, descriptor, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        for (AbstractBleProfile profile : mSupportedProfiles) {
            if (profile.onCharacteristicChanged(gatt, characteristic)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        for (AbstractBleProfile profile : mSupportedProfiles) {
            profile.onReadRemoteRssi(gatt, rssi, status);
        }
    }
}

/*  Copyright (C) 2015-2024 Andreas Böhler, Arjan Schrijver, Carsten Pfeiffer,
    Daniel Dakhno, Daniele Gobbetti, Johannes Krude, JohnnySun, José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.CheckInitializedAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;

/**
 * Abstract base class for all devices connected through Bluetooth Low Energy (LE) aka
 * Bluetooth Smart.
 * <p/>
 * The connection to the device and all communication is made with a generic {@link BtLEQueue}.
 * Messages to the device are encoded as {@link BtLEAction actions} or {@link BtLEServerAction actions}
 * that are grouped with a {@link Transaction} or {@link ServerTransaction} and sent via {@link BtLEQueue}.
 *
 * @see TransactionBuilder
 * @see BtLEQueue
 */
public abstract class AbstractBTLEDeviceSupport extends AbstractDeviceSupport implements GattCallback, GattServerCallback {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBTLEDeviceSupport.class);

    private int mMTU = 23;
    private BtLEQueue mQueue;
    private Map<UUID, BluetoothGattCharacteristic> mAvailableCharacteristics;
    private final Set<UUID> mSupportedServices = new HashSet<>(4);
    private final Set<BluetoothGattService> mSupportedServerServices = new HashSet<>(4);
    private final Logger logger;

    private final List<AbstractBleProfile<?>> mSupportedProfiles = new ArrayList<>();
    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb"; //this is common for all BTLE devices. see http://stackoverflow.com/questions/18699251/finding-out-android-bluetooth-le-gatt-profiles
    private final Object characteristicsMonitor = new Object();

    private BleIntentApi bleApi = null;

    public AbstractBTLEDeviceSupport(Logger logger) {
        this.logger = logger;
        if (logger == null) {
            throw new IllegalArgumentException("logger must not be null");
        }
    }

    @Override
    public boolean connect() {
        if (mQueue == null) {
            mQueue = new BtLEQueue(getBluetoothAdapter(), getDevice(), this, this, getContext(), mSupportedServerServices);
            if(bleApi != null) {
                bleApi.setQueue(mQueue);
            }
            mQueue.setAutoReconnect(getAutoReconnect());
            mQueue.setScanReconnect(getScanReconnect());
            mQueue.setImplicitGattCallbackModify(getImplicitCallbackModify());
            mQueue.setSendWriteRequestResponse(getSendWriteRequestResponse());
        }

        return mQueue.connect();
    }

    public void disconnect() {
        if (mQueue != null) {
            mQueue.disconnect();
        }
    }

    public BleIntentApi getBleApi() {
        return bleApi;
    }

    @Override
    public void onSendConfiguration(String config) {
        if(bleApi != null) {
            bleApi.onSendConfiguration(config);
        }
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        super.setContext(gbDevice, btAdapter, context);

        if(BleIntentApi.isEnabled(gbDevice)) {
            bleApi = new BleIntentApi(context, gbDevice);
            bleApi.handleBLEApiPrefs();
        }
    }



    /**
     * Returns whether the gatt callback should be implicitly set to the one on the transaction,
     * even if it was not set directly on the transaction. If true, the gatt callback will always
     * be set to the one in the transaction, even if null and not explicitly set to null.
     * See <a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/2912">#2912</a> for
     * more information. This is false by default, but we are making it configurable to avoid breaking
     * older devices that rely on this behavior, so all older devices got this overridden to true.
     */
    public boolean getImplicitCallbackModify() {
        return false;
    }

    /**
     * Whether to send a write request response to the device, if requested. The standard actually
     * expects this to happen, but Gadgetbridge did not originally support it. This is set to true
     * on all older devices that were not confirmed to handle the response well after this was introduced.
     * <p>
     * See also: <a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/2831#issuecomment-941568">#2831#issuecomment-941568</a>
     *
     * @return whether to send write request responses, if a response is requested
     */
    public boolean getSendWriteRequestResponse() {
        return true;
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

        if(bleApi != null) {
            bleApi.dispose();
        }
    }

    public TransactionBuilder createTransactionBuilder(String taskName) {
        return new TransactionBuilder(taskName);
    }

    /**
     * Send commands like this to the device:
     * <p>
     * <code>performInitialized("sms notification").write(someCharacteristic, someByteArray).queue(getQueue());</code>
     * </p>
     * This will asynchronously
     * <ul>
     * <li>connect to the device (if necessary)</li>
     * <li>initialize the device (if necessary)</li>
     * <li>execute the commands collected with the returned transaction builder</li>
     * </ul>
     *
     * @see #performConnected(Transaction)
     * @see #initializeDevice(TransactionBuilder)
     */
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        if (!isConnected()) {
            LOG.debug("Connecting to device for {}", taskName);
            if (!connect()) {
                throw new IOException("1: Unable to connect to device: " + getDevice());
            }
        }
        if (!isInitialized()) {
            LOG.debug("Initializing device for {}", taskName);
            // first, add a transaction that performs device initialization
            TransactionBuilder builder = createTransactionBuilder("Initialize device");
            builder.add(new CheckInitializedAction(gbDevice));
            initializeDevice(builder).queue(getQueue());
        }
        return createTransactionBuilder(taskName);
    }

    public ServerTransactionBuilder createServerTransactionBuilder(String taskName) {
        return new ServerTransactionBuilder(taskName);
    }

    public ServerTransactionBuilder performServer(String taskName) throws IOException {
        if (!isConnected()) {
            if(!connect()) {
                throw new IOException("1: Unable to connect to device: " + getDevice());
            }
        }
        return createServerTransactionBuilder(taskName);
    }

    /**
     * Ensures that the device is connected and (only then) performs the actions of the given
     * transaction builder.
     * <p>
     * In contrast to {@link #performInitialized(String)}, no initialization sequence is performed
     * with the device, only the actions of the given builder are executed.
     * @throws IOException if unable to connect to the device
     * @see #performInitialized(String)
     */
    public void performConnected(Transaction transaction) throws IOException {
        if (!isConnected()) {
            if (!connect()) {
                throw new IOException("2: Unable to connect to device: " + getDevice());
            }
        }
        getQueue().add(transaction);
    }

    /**
     * Performs the actions of the given transaction as soon as possible,
     * that is, before any other queued transactions, but after the actions
     * of the currently executing transaction.
     */
    public void performImmediately(TransactionBuilder builder) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to device: " + getDevice());
        }
        getQueue().insert(builder.getTransaction());
    }

    public BtLEQueue getQueue() {
        return mQueue;
    }

    /**
     * Subclasses should call this method to add services they support.
     * Only supported services will be queried for characteristics.
     *
     * @param aSupportedService supported service uuid
     * @see #getCharacteristic(UUID)
     */
    protected void addSupportedService(UUID aSupportedService) {
        mSupportedServices.add(aSupportedService);
    }

    protected void addSupportedProfile(AbstractBleProfile<?> profile) {
        mSupportedProfiles.add(profile);
    }

    /**
     * Subclasses should call this method to add server services they support.
     */
    protected void addSupportedServerService(BluetoothGattService service) {
        mSupportedServerServices.add(service);
    }

    /**
     * Returns the characteristic matching the given UUID. Only characteristics
     * are returned whose service is marked as supported.
     *
     * @param uuid characteristic uuid
     * @return the characteristic for the given UUID or <code>null</code>
     * @see #addSupportedService(UUID)
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        synchronized (characteristicsMonitor) {
            if (mAvailableCharacteristics == null) {
                return null;
            }
            return mAvailableCharacteristics.get(uuid);
        }
    }

    private void gattServicesDiscovered(List<BluetoothGattService> discoveredGattServices) {
        if (discoveredGattServices == null) {
            logger.warn("No gatt services discovered: null!");
            return;
        }
        Set<UUID> supportedServices = getSupportedServices();
        Map<UUID, BluetoothGattCharacteristic> newCharacteristics = new HashMap<>();
        for (BluetoothGattService service : discoveredGattServices) {
            if(bleApi != null) {
                bleApi.addService(service);
            }

            if (supportedServices.contains(service.getUuid())) {
                logger.debug("discovered supported service: {}: {}", BleNamesResolver.resolveServiceName(service.getUuid().toString()), service.getUuid());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                if (characteristics == null || characteristics.isEmpty()) {
                    logger.warn("Supported LE service {} did not return any characteristics", service.getUuid());
                    continue;
                }
                HashMap<UUID, BluetoothGattCharacteristic> intmAvailableCharacteristics = new HashMap<>(characteristics.size());
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    intmAvailableCharacteristics.put(characteristic.getUuid(), characteristic);
                    logger.info("    characteristic: {}: {}", BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()), characteristic.getUuid());
                }
                newCharacteristics.putAll(intmAvailableCharacteristics);

                synchronized (characteristicsMonitor) {
                    mAvailableCharacteristics = newCharacteristics;
                }
            } else {
                logger.debug("discovered unsupported service: {}: {}", BleNamesResolver.resolveServiceName(service.getUuid().toString()), service.getUuid());
            }
        }
    }

    protected Set<UUID> getSupportedServices() {
        return mSupportedServices;
    }

    /**
     * Utility method that may be used to log incoming messages when we don't know how to deal with them yet.
     */
    public void logMessageContent(byte[] value) {
        logger.info("RECEIVED DATA WITH LENGTH: {}", (value != null) ? value.length : "(null)");
        Logging.logBytes(logger, value);
    }

    // default implementations of event handler methods (gatt callbacks)
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            profile.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        gattServicesDiscovered(gatt.getServices());

        if (getDevice().getState().compareTo(GBDevice.State.INITIALIZING) >= 0) {
            logger.warn("Services discovered, but device state is already " + getDevice().getState() + " for device: " + getDevice() + ", so ignoring");
            return;
        }
        TransactionBuilder builder = createTransactionBuilder("Initializing device");

        if(bleApi != null) {
            bleApi.initializeDevice(builder);
        }

        initializeDevice(builder).queue(getQueue());
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {
        if(bleApi != null) {
            bleApi.onCharacteristicChanged(characteristic);
        }

        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onCharacteristicRead(gatt, characteristic, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onCharacteristicWrite(gatt, characteristic, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onDescriptorRead(gatt, descriptor, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onDescriptorWrite(gatt, descriptor, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if(bleApi != null) {
            bleApi.onCharacteristicChanged(characteristic);
        }

        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onCharacteristicChanged(gatt, characteristic)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            profile.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        this.mMTU = mtu;
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {

    }

    @Override
    public boolean onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    public boolean onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        return false;
    }

    @Override
    public boolean onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        return false;
    }

    @Override
    public boolean onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        return false;
    }

    /**
     * Gets the current MTU, or 0 if unknown
     * @return the current MTU, 0 if unknown
     */
    public int getMTU() {
        return mMTU;
    }
}

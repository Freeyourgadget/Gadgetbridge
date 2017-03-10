/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, JohnnySun

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * Messages to the device are encoded as {@link BtLEAction actions} that are grouped with a
 * {@link Transaction} and sent via {@link BtLEQueue}.
 *
 * @see TransactionBuilder
 * @see BtLEQueue
 */
public abstract class AbstractBTLEDeviceSupport extends AbstractDeviceSupport implements GattCallback {
    private BtLEQueue mQueue;
    private Map<UUID, BluetoothGattCharacteristic> mAvailableCharacteristics;
    private final Set<UUID> mSupportedServices = new HashSet<>(4);
    private Logger logger;

    private final List<AbstractBleProfile<?>> mSupportedProfiles = new ArrayList<>();
    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb"; //this is common for all BTLE devices. see http://stackoverflow.com/questions/18699251/finding-out-android-bluetooth-le-gatt-profiles
    private final Object characteristicsMonitor = new Object();

    public AbstractBTLEDeviceSupport(Logger logger) {
        this.logger = logger;
        if (logger == null) {
            throw new IllegalArgumentException("logger must not be null");
        }
    }

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

    public TransactionBuilder createTransactionBuilder(String taskName) {
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
     * @param builder
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
            if (supportedServices.contains(service.getUuid())) {
                logger.debug("discovered supported service: " + BleNamesResolver.resolveServiceName(service.getUuid().toString()) + ": " + service.getUuid());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                if (characteristics == null || characteristics.isEmpty()) {
                    logger.warn("Supported LE service " + service.getUuid() + "did not return any characteristics");
                    continue;
                }
                HashMap<UUID, BluetoothGattCharacteristic> intmAvailableCharacteristics = new HashMap<>(characteristics.size());
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    intmAvailableCharacteristics.put(characteristic.getUuid(), characteristic);
                    logger.info("    characteristic: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()) + ": " + characteristic.getUuid());
                }
                newCharacteristics.putAll(intmAvailableCharacteristics);

                synchronized (characteristicsMonitor) {
                    mAvailableCharacteristics = newCharacteristics;
                }
            } else {
                logger.debug("discovered unsupported service: " + BleNamesResolver.resolveServiceName(service.getUuid().toString()) + ": " + service.getUuid());
            }
        }
    }

    protected Set<UUID> getSupportedServices() {
        return mSupportedServices;
    }

    /**
     * Utility method that may be used to log incoming messages when we don't know how to deal with them yet.
     *
     * @param value
     */
    public void logMessageContent(byte[] value) {
        logger.info("RECEIVED DATA WITH LENGTH: " + ((value != null) ? value.length : "(null)"));
        Logging.logBytes(logger, value);
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

        if (getDevice().getState().compareTo(GBDevice.State.INITIALIZING) >= 0) {
            logger.warn("Services discovered, but device state is already " + getDevice().getState() + " for device: " + getDevice() + ", so ignoring");
            return;
        }
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

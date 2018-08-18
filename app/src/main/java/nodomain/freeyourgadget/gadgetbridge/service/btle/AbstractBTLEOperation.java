/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Uwe Hermann

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
import android.content.Context;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

/**
 * Abstract base class for a BTLEOperation, i.e. an operation that does more than
 * just sending a few bytes to the device. It typically involves exchanging many messages
 * between the mobile and the device.
 * <p/>
 * One operation may execute multiple @{link Transaction transactions} with each
 * multiple @{link BTLEAction actions}.
 * <p/>
 * This class implements GattCallback so that subclasses may override those methods
 * to handle those events.
 * Note: by default all Gatt events are forwarded to AbstractBTLEDeviceSupport, subclasses may override
 * this behavior.
 */
public abstract class AbstractBTLEOperation<T extends AbstractBTLEDeviceSupport> implements GattCallback, BTLEOperation {
    private final T mSupport;
    protected OperationStatus operationStatus = OperationStatus.INITIAL;
    private String name;

    protected AbstractBTLEOperation(T support) {
        mSupport = support;
    }

    /**
     * Performs this operation. The whole operation is asynchronous, i.e.
     * this method quickly returns before the actual operation is finished.
     * Calls #prePerform() and, if successful, #doPerform().
     *
     * @throws IOException
     */
    @Override
    public final void perform() throws IOException {
        operationStatus = OperationStatus.STARTED;
        prePerform();
        operationStatus = OperationStatus.RUNNING;
        doPerform();
    }

    /**
     * Hook for subclasses to perform something before #doPerform() is invoked.
     *
     * @throws IOException
     */
    protected void prePerform() throws IOException {
    }

    /**
     * Subclasses must implement this. When invoked, #prePerform() returned
     * successfully.
     * Note that subclasses HAVE TO call #operationFinished() when the entire
     * operation is done (successful or not).
     *
     * @throws IOException
     */
    protected abstract void doPerform() throws IOException;

    /**
     * You MUST call this method when the operation has finished, either
     * successfully or unsuccessfully.
     *
     * Subclasses must ensure that the {@link BtLEQueue queue's}'s gatt callback (set on the transaction builder by {@link #performInitialized(String)})
     * is being unset, otherwise it will continue to receive events until another transaction is being executed by the queue.
     *
     * @throws IOException
     */
    protected void operationFinished() throws IOException {
    }

    /**
     * Delegates to the DeviceSupport instance and additionally sets this instance as the Gatt
     * callback for the transaction.
     *
     * @param taskName
     * @return
     * @throws IOException
     */
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        TransactionBuilder builder = mSupport.performInitialized(taskName);
        builder.setGattCallback(this);
        return builder;
    }

    protected Context getContext() {
        return mSupport.getContext();
    }

    protected GBDevice getDevice() {
        return mSupport.getDevice();
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        String busyTask = getDevice().getBusyTask();
        if (busyTask != null) {
            return busyTask;
        }
        return getClass().getSimpleName();
    }

    protected BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        return mSupport.getCharacteristic(uuid);
    }

    protected BtLEQueue getQueue() {
        return mSupport.getQueue();
    }

    protected void unsetBusy() {
        if (getDevice().isBusy()) {
            getDevice().unsetBusyTask();
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    public boolean isOperationRunning() {
        return operationStatus == OperationStatus.RUNNING;
    }

    public boolean isOperationFinished() {
        return operationStatus == OperationStatus.FINISHED;
    }

    public T getSupport() {
        return mSupport;
    }

    // All Gatt callbacks delegated to MiBandSupport
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        mSupport.onConnectionStateChange(gatt, status, newState);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        mSupport.onServicesDiscovered(gatt);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        return mSupport.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        return mSupport.onCharacteristicWrite(gatt, characteristic, status);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return mSupport.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        return mSupport.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        return mSupport.onDescriptorWrite(gatt, descriptor, status);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        mSupport.onReadRemoteRssi(gatt, rssi, status);
    }
}

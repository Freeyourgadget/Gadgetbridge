package nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCallback;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;

/**
 * Abstract base class for a MiBandOperation, i.e. an operation that does more than
 * just sending a few bytes to the band. It typically involves exchanging many messages
 * between the mobile and the band.
 *
 * One operation may execute multiple @{link Transaction transactions} with each
 * multiple @{link BTLEAction actions}.
 *
 * This class implements GattCallback so that subclasses may override those methods
 * to handle those events.
 * Note: by default all Gatt events are forwarded to MiBandSupport, subclasses may override
 * this behavior.
 */
public abstract class AbstractMiBandOperation implements GattCallback, MiBandOperation {
    private final MiBandSupport mSupport;

    protected AbstractMiBandOperation(MiBandSupport support) {
        mSupport = support;
    }

    /**
     * Delegates to MiBandSupport and additionally sets this instance as the Gatt
     * callback for the transaction.
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

    protected BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        return mSupport.getCharacteristic(uuid);
    }

    protected BtLEQueue getQueue() {
        return mSupport.getQueue();
    }

    protected void unsetBusy() {
        getDevice().unsetBusyTask();
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    public MiBandSupport getSupport() {
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
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        mSupport.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        mSupport.onCharacteristicWrite(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        mSupport.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        mSupport.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        mSupport.onDescriptorWrite(gatt, descriptor, status);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        mSupport.onReadRemoteRssi(gatt, rssi, status);
    }
}

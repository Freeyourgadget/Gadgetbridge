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
     * opreation is done (successful or not).
     *
     * @throws IOException
     */
    protected abstract void doPerform() throws IOException;

    /**
     * You MUST call this method when the operation has finished, either
     * successfull or unsuccessfully.
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

    public boolean isOperationRunning() {
        return operationStatus == OperationStatus.RUNNING;
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

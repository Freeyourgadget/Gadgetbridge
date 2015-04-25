package nodomain.freeyourgadget.gadgetbridge.btle;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import nodomain.freeyourgadget.gadgetbridge.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.GBDevice.State;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

/**
 * One queue/thread per connectable device.
 */
public final class BtLEQueue {
    private static final String TAG = BtLEQueue.class.getSimpleName();

    private GBDevice mGbDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private volatile BlockingQueue<Transaction> mTransactions = new LinkedBlockingQueue<Transaction>();
    private volatile boolean mDisposed;
    private volatile boolean mCrashed;
    private volatile boolean mAbortTransaction;

    private Context mContext;
    private CountDownLatch mWaitForActionResultLatch;
    private CountDownLatch mConnectionLatch;
    private BluetoothGattCharacteristic mWaitCharacteristic;
    private GattCallback mExternalGattCallback;

    private Thread dispatchThread = new Thread("Bluetooth GATT Dispatcher") {

        @Override
        public void run() {
            Log.d(TAG, "Queue Dispatch Thread started.");

            while (!mDisposed && !mCrashed) {
                try {
                    Transaction transaction = mTransactions.take();
                    if (!isConnected()) {
                        // TODO: request connection and initialization from the outside and wait until finished

                        // wait until the connection succeeds before running the actions
                        // Note that no automatic connection is performed. This has to be triggered
                        // on the outside typically by the DeviceSupport. The reason is that
                        // devices have different kinds of initializations and this class has no
                        // idea about them.
                        mConnectionLatch = new CountDownLatch(1);
                        mConnectionLatch.await();
                        mConnectionLatch = null;
                    }

                    mAbortTransaction = false;
                    // Run all actions of the transaction until one doesn't succeed
                    for (BtLEAction action : transaction.getActions()) {
                        mWaitCharacteristic = action.getCharacteristic();
                        if (action.run(mBluetoothGatt)) {
                            mWaitForActionResultLatch = new CountDownLatch(1);
                            mWaitForActionResultLatch.await();
                            mWaitForActionResultLatch = null;
                            if (mAbortTransaction) {
                                break;
                            }
                        } else {
                            Log.e(TAG, "Action returned false: " + action);
                            break; // abort the transaction
                        }
                    }
                } catch (InterruptedException ignored) {
                    mWaitForActionResultLatch = null;
                    mConnectionLatch = null;
                    Log.d(TAG, "Thread interrupted");
                } catch (Throwable ex) {
                    Log.e(TAG, "Queue Dispatch Thread died: " + ex.getMessage());
                    mCrashed = true;
                    mWaitForActionResultLatch = null;
                    mConnectionLatch = null;
                } finally {
                    mWaitCharacteristic = null;
                }
            }
            Log.i(TAG, "Queue Dispatch Thread terminated.");
        }
    };

    public BtLEQueue(BluetoothAdapter bluetoothAdapter, GBDevice gbDevice, GattCallback externalGattCallback, Context context) {
        mBluetoothAdapter = bluetoothAdapter;
        mGbDevice = gbDevice;
        mExternalGattCallback = externalGattCallback;
        mContext = context;

        dispatchThread.start();
    }

    protected boolean isConnected() {
        return mGbDevice.isConnected();
    }

    /**
     * Connects to the given remote device. Note that this does not perform any device
     * specific initialization. This should be done in the specific {@link DeviceSupport}
     * class.
     *
     * @return <code>true</code> whether the connection attempt was successfully triggered and <code>false</code> if that failed or if there is already a connection
     */
    public boolean connect() {
        if (isConnected()) {
            Log.w(TAG, "Ingoring connect() because already connected.");
            return false;
        }
        Log.i(TAG, "Attempting to connect to " + mGbDevice.getName());
        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mGbDevice.getAddress());
        mBluetoothGatt = remoteDevice.connectGatt(mContext, false, internalGattCallback);
        boolean result = mBluetoothGatt.connect();
        setDeviceConnectionState(result ? State.CONNECTING : State.NOT_CONNECTED);
        return result;
    }

    private void setDeviceConnectionState(State newState) {
        mGbDevice.setState(newState);
        mGbDevice.sendDeviceUpdateIntent(mContext);
        if (mConnectionLatch != null) {
            mConnectionLatch.countDown();
        }
    }

    public void disconnect() {
        if (mBluetoothGatt != null) {
            Log.i(TAG, "Disconnecting BtLEQueue from GATT device");
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    private void handleDisconnected() {
        mTransactions.clear();
        if (mWaitForActionResultLatch != null) {
            mWaitForActionResultLatch.countDown();
        }
        setDeviceConnectionState(State.NOT_CONNECTED);
    }

    public void dispose() {
        if (mDisposed) {
            return;
        }
        mDisposed = true;
//        try {
        disconnect();
        dispatchThread.interrupt();
        dispatchThread = null;
//            dispatchThread.join();
//        } catch (InterruptedException ex) {
//            Log.e(TAG, "Exception while disposing BtLEQueue", ex);
//        }
    }

    /**
     * Adds a transaction to the end of the queue.
     *
     * @param transaction
     */
    public void add(Transaction transaction) {
        Log.d(TAG, "about to add: " + transaction);
        if (!transaction.isEmpty()) {
            mTransactions.add(transaction);
        }
        Log.d(TAG, "adding done: " + transaction);
    }

    public void clear() {
        mTransactions.clear();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt is null => no services available.");
            return Collections.emptyList();
        }
        return mBluetoothGatt.getServices();
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback internalGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "Connected to GATT server.");
                    setDeviceConnectionState(State.CONNECTED);
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" +
                            mBluetoothGatt.discoverServices());
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected from GATT server.");
                    handleDisconnected();
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    Log.i(TAG, "Connecting to GATT server...");
                    setDeviceConnectionState(State.CONNECTING);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mExternalGattCallback != null) {
                    // only propagate the successful event
                    mExternalGattCallback.onServicesDiscovered(gatt);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Writing characteristic " + characteristic.getUuid() + " succeeded.");
            } else {
                Log.e(TAG, "Writing characteristic " + characteristic.getUuid() + " failed: " + status);
            }
            if (mExternalGattCallback != null) {
                mExternalGattCallback.onCharacteristicWrite(gatt, characteristic, status);
            }
            checkWaitingCharacteristic(characteristic, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Reading characteristic " + characteristic.getUuid() + " failed: " + status);
            }
            if (mExternalGattCallback != null) {
                mExternalGattCallback.onCharacteristicRead(gatt, characteristic, status);
            }
            checkWaitingCharacteristic(characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (mExternalGattCallback != null) {
                mExternalGattCallback.onCharacteristicChanged(gatt, characteristic);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (mExternalGattCallback != null) {
                mExternalGattCallback.onReadRemoteRssi(gatt, rssi, status);
            }
        }

        private void checkWaitingCharacteristic(BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mAbortTransaction = true;
            }
            if (characteristic != null && BtLEQueue.this.mWaitCharacteristic != null && characteristic.getUuid().equals(BtLEQueue.this.mWaitCharacteristic.getUuid())) {
                if (mWaitForActionResultLatch != null) {
                    mWaitForActionResultLatch.countDown();
                }
            }
        }
    };
}

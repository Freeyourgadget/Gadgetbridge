/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Sergey Trofimov, Uwe Hermann

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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import androidx.annotation.Nullable;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothScanCallbackReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;

/**
 * One queue/thread per connectable device.
 */
public final class BtLEQueue {
    private static final Logger LOG = LoggerFactory.getLogger(BtLEQueue.class);

    private final Object mGattMonitor = new Object();
    private final Object mTransactionMonitor = new Object();
    private final GBDevice mGbDevice;
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattServer mBluetoothGattServer;
    private final Set<BluetoothGattService> mSupportedServerServices;

    private final Queue<Transaction> mTransactions = new ConcurrentLinkedQueue<>();
    private final Queue<ServerTransaction> mServerTransactions = new ConcurrentLinkedQueue<>();
    private volatile boolean mDisposed;
    private volatile boolean mCrashed;
    private volatile boolean mAbortTransaction;
    private volatile boolean mAbortServerTransaction;

    private final Handler mHandler = new Handler();

    private final Context mContext;
    private CountDownLatch mWaitForActionResultLatch;
    private CountDownLatch mWaitForServerActionResultLatch;
    private CountDownLatch mConnectionLatch;
    private BluetoothGattCharacteristic mWaitCharacteristic;
    private final InternalGattCallback internalGattCallback;
    private final InternalGattServerCallback internalGattServerCallback;
    private boolean mAutoReconnect = false;

    private BluetoothLeScanner mBluetoothScanner;
    private boolean mUseBleScannerForReconnect = false;
    private PendingIntent mScanCallbackIntent = null;

    private Runnable mRestartRunnable = new Runnable() {
        @Override
        public void run() {
            LOG.info("Restarting background scan due to Android N limitations...");
            startBleBackgroundScan();
        }
    };

    private Runnable mReduceBleScanIntervalRunnable = new Runnable() {
        @Override
        public void run() {
            LOG.info("Restarting BLE background scan with lower priority...");
            startBleBackgroundScan(false);
        }
    };

    private Thread dispatchThread = new Thread("Gadgetbridge GATT Dispatcher") {

        @Override
        public void run() {
            LOG.debug("Queue Dispatch Thread started.");

            while (!mDisposed && !mCrashed) {
                try {
                    if(mTransactions.isEmpty() && mServerTransactions.isEmpty()) {
                        synchronized (mTransactionMonitor) {
                            try {
                                mTransactionMonitor.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Transaction transaction = mTransactions.poll();
                    ServerTransaction serverTransaction = mServerTransactions.poll();

                    if (!isConnected()) {
                        LOG.debug("not connected, waiting for connection...");
                        // TODO: request connection and initialization from the outside and wait until finished
                        internalGattCallback.reset();

                        // wait until the connection succeeds before running the actions
                        // Note that no automatic connection is performed. This has to be triggered
                        // on the outside typically by the DeviceSupport. The reason is that
                        // devices have different kinds of initializations and this class has no
                        // idea about them.
                        mConnectionLatch = new CountDownLatch(1);
                        mConnectionLatch.await();
                        mConnectionLatch = null;
                    }

                    if(serverTransaction != null) {
                        internalGattServerCallback.setTransactionGattCallback(serverTransaction.getGattCallback());
                        mAbortServerTransaction = false;

                        for (BtLEServerAction action : serverTransaction.getActions()) {
                            if (mAbortServerTransaction) { // got disconnected
                                LOG.info("Aborting running transaction");
                                break;
                            }
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("About to run action: " + action);
                            }
                            if (action.run(mBluetoothGattServer)) {
                                // check again, maybe due to some condition, action did not need to write, so we can't wait
                                boolean waitForResult = action.expectsResult();
                                if (waitForResult) {
                                    mWaitForServerActionResultLatch.await();
                                    mWaitForServerActionResultLatch = null;
                                    if (mAbortServerTransaction) {
                                        break;
                                    }
                                }
                            } else {
                                LOG.error("Action returned false: " + action);
                                break; // abort the transaction
                            }
                        }
                    }

                    if(transaction != null) {
                        internalGattCallback.setTransactionGattCallback(transaction.getGattCallback());
                        mAbortTransaction = false;
                        // Run all actions of the transaction until one doesn't succeed
                        for (BtLEAction action : transaction.getActions()) {
                            if (mAbortTransaction) { // got disconnected
                                LOG.info("Aborting running transaction");
                                break;
                            }
                            mWaitCharacteristic = action.getCharacteristic();
                            mWaitForActionResultLatch = new CountDownLatch(1);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("About to run action: " + action);
                            }
                            if (action instanceof GattListenerAction) {
                                // this special action overwrites the transaction gatt listener (if any), it must
                                // always be the last action in the transaction
                                internalGattCallback.setTransactionGattCallback(((GattListenerAction) action).getGattCallback());
                            }
                            if (action.run(mBluetoothGatt)) {
                                // check again, maybe due to some condition, action did not need to write, so we can't wait
                                boolean waitForResult = action.expectsResult();
                                if (waitForResult) {
                                    mWaitForActionResultLatch.await();
                                    mWaitForActionResultLatch = null;
                                    if (mAbortTransaction) {
                                        break;
                                    }
                                }
                            } else {
                                LOG.error("Action returned false: " + action);
                                break; // abort the transaction
                            }
                        }
                    }
                } catch (InterruptedException ignored) {
                    mConnectionLatch = null;
                    LOG.debug("Thread interrupted");
                } catch (Throwable ex) {
                    LOG.error("Queue Dispatch Thread died: " + ex.getMessage(), ex);
                    mCrashed = true;
                    mConnectionLatch = null;
                } finally {
                    mWaitForActionResultLatch = null;
                    mWaitCharacteristic = null;
                }
            }
            LOG.info("Queue Dispatch Thread terminated.");
        }
    };

    public BtLEQueue(BluetoothAdapter bluetoothAdapter, GBDevice gbDevice, GattCallback externalGattCallback, GattServerCallback externalGattServerCallback, Context context, Set<BluetoothGattService> supportedServerServices) {
        mBluetoothAdapter = bluetoothAdapter;
        mGbDevice = gbDevice;
        internalGattCallback = new InternalGattCallback(externalGattCallback);
        internalGattServerCallback = new InternalGattServerCallback(externalGattServerCallback);
        mContext = context;
        mSupportedServerServices = supportedServerServices;

        dispatchThread.start();
    }

    public void setAutoReconnect(boolean enable) {
        mAutoReconnect = enable;
    }

    public void setBleScannerForReconnect(boolean enable) {
        mUseBleScannerForReconnect = enable;
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
            LOG.warn("Ingoring connect() because already connected.");
            return false;
        }
        synchronized (mGattMonitor) {
            if (mBluetoothGatt != null) {
                // Tribal knowledge says you're better off not reusing existing BluetoothGatt connections,
                // so create a new one.
                LOG.info("connect() requested -- disconnecting previous connection: " + mGbDevice.getName());
                disconnect();
            }
        }
        LOG.info("Attempting to connect to " + mGbDevice.getName());
        mBluetoothAdapter.cancelDiscovery();
        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mGbDevice.getAddress());
        if(!mSupportedServerServices.isEmpty()) {
            BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                LOG.error("Error getting bluetoothManager");
                return false;
            }
            mBluetoothGattServer = bluetoothManager.openGattServer(mContext, internalGattServerCallback);
            if (mBluetoothGattServer == null) {
                LOG.error("Error opening Gatt Server");
                return false;
            }
            for(BluetoothGattService service : mSupportedServerServices) {
                mBluetoothGattServer.addService(service);
            }
        }
        synchronized (mGattMonitor) {
            // connectGatt with true doesn't really work ;( too often connection problems
            if (GBApplication.isRunningMarshmallowOrLater()) {
                mBluetoothGatt = remoteDevice.connectGatt(mContext, false, internalGattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mBluetoothGatt = remoteDevice.connectGatt(mContext, false, internalGattCallback);
            }
        }
        boolean result = mBluetoothGatt != null;
        if (result) {
            setDeviceConnectionState(State.CONNECTING);
        }
        return result;
    }

    private void setDeviceConnectionState(State newState) {
        LOG.debug("new device connection state: " + newState);
        mGbDevice.setState(newState);
        mGbDevice.sendDeviceUpdateIntent(mContext);
        if (mConnectionLatch != null && newState == State.CONNECTED) {
            mConnectionLatch.countDown();
        }
    }

    public void disconnect() {
        synchronized (mGattMonitor) {
            LOG.debug("disconnect()");

            BluetoothGattServer gattServer = mBluetoothGattServer;
            if (gattServer != null) {
                mBluetoothGattServer = null;
                BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager == null) {
                    LOG.error("Error getting bluetoothManager");
                } else {
                    List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
                    for(BluetoothDevice device : devices) {
                        LOG.debug("Disconnecting device: " + device.getAddress());
                        gattServer.cancelConnection(device);
                    }
                }
                gattServer.clearServices();
                gattServer.close();
            }

            BluetoothGatt gatt = mBluetoothGatt;
            if (gatt != null) {
                mBluetoothGatt = null;
                LOG.info("Disconnecting BtLEQueue from GATT device");
                gatt.disconnect();
                gatt.close();
                setDeviceConnectionState(State.NOT_CONNECTED);
            }
        }
    }

    private void handleDisconnected(int status) {
        LOG.debug("handleDisconnected: " + status);
        internalGattCallback.reset();
        mTransactions.clear();
        mServerTransactions.clear();
        mAbortTransaction = true;
        mAbortServerTransaction = true;
        if (mWaitForActionResultLatch != null) {
            mWaitForActionResultLatch.countDown();
        }
        if (mWaitForServerActionResultLatch != null) {
            mWaitForServerActionResultLatch.countDown();
        }
        synchronized(mTransactionMonitor) {
            mTransactionMonitor.notify();
        }
        setDeviceConnectionState(State.NOT_CONNECTED);

        // either we've been disconnected because the device is out of range
        // or because of an explicit @{link #disconnect())
        // To support automatic reconnection, we keep the mBluetoothGatt instance
        // alive (we do not close() it). Unfortunately we sometimes have problems
        // reconnecting automatically, so we try to fix this by re-creating mBluetoothGatt.
        // Not sure if this actually works without re-initializing the device...
        if (mBluetoothGatt != null) {
            if (!maybeReconnect()) {
                disconnect(); // ensure that we start over cleanly next time
            }
        }
    }

    /**
     * Depending on certain criteria, connects to the BluetoothGatt.
     *
     * @return true if a reconnection attempt was made, or false otherwise
     */
    private boolean maybeReconnect() {
        if (mAutoReconnect && mBluetoothGatt != null) {
            if(!mUseBleScannerForReconnect) {
                LOG.info("Enabling automatic ble reconnect...");
                boolean result = mBluetoothGatt.connect();
                if (result) {
                    setDeviceConnectionState(State.WAITING_FOR_RECONNECT);
                }
                return result;
            } else {
                if(GBApplication.isRunningLollipopOrLater()) {
                    LOG.info("Enabling BLE background scan");
                    disconnect(); // ensure that we start over cleanly next time
                    startBleBackgroundScan();
                    setDeviceConnectionState(State.WAITING_FOR_RECONNECT);
                    return true;
                }
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.O)
    PendingIntent getScanCallbackIntent(boolean newUuid) {
        if(newUuid || mScanCallbackIntent == null) {
            String uuid = UUID.randomUUID().toString();
            mScanCallbackIntent = BluetoothScanCallbackReceiver.getScanCallbackIntent(mContext, mGbDevice.getAddress(), uuid);
        }
        return mScanCallbackIntent;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void stopBleBackgroundScan() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mHandler.removeCallbacks(mReduceBleScanIntervalRunnable);
            mBluetoothScanner.stopScan(getScanCallbackIntent(false));
        } else {
            mHandler.removeCallbacks(mRestartRunnable);
            mBluetoothScanner.stopScan(mScanCallback);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startBleBackgroundScan() {
        startBleBackgroundScan(true);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startBleBackgroundScan(boolean highPowerMode) {
        if(mBluetoothScanner == null)
            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();

        ScanSettings settings;
        if(highPowerMode) {
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
        } else {
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LOG.info("Using Android O+ BLE scanner");
            List<ScanFilter> filters = Collections.singletonList(new ScanFilter.Builder().build());
            mBluetoothScanner.stopScan(getScanCallbackIntent(false));
            mBluetoothScanner.startScan(filters, settings, getScanCallbackIntent(true));
            // If high power mode is requested, we scan for 5 minutes
            // and then continue scanning with lower priority (scan mode balanced) in order
            // to conserve power.
            if(highPowerMode) {
                mHandler.postDelayed(mReduceBleScanIntervalRunnable, 5 * 60 * 1000);
            }
        }
        else {
            LOG.info("Using Android L-N BLE scanner");
            List<ScanFilter> filters = Collections.singletonList(new ScanFilter.Builder().setDeviceAddress(mGbDevice.getAddress()).build());
            mBluetoothScanner.stopScan(mScanCallback);
            mBluetoothScanner.startScan(filters, settings, mScanCallback);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mHandler.postDelayed(mRestartRunnable, 25 * 60 * 1000);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String deviceName = result.getDevice().getName();
            String deviceAddress = result.getDevice().getAddress();

            LOG.info("Scanner: Found: " + deviceName + " " + deviceAddress);
            // The filter already filtered for our specific device, so it is enough to connect to it
            mBluetoothScanner.stopScan(mScanCallback);
            mHandler.removeCallbacks(mRestartRunnable);
            connect();
            setDeviceConnectionState(State.CONNECTING);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                LOG.info("ScanCallback.onBatchScanResults.each:" + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            LOG.error("ScanCallback.onScanFailed:" + errorCode);
        }
    };

    public void dispose() {
        if (mDisposed) {
            return;
        }
        mDisposed = true;
//        try {
        disconnect();
        stopBleBackgroundScan();
        dispatchThread.interrupt();
        dispatchThread = null;
//            dispatchThread.join();
//        } catch (InterruptedException ex) {
//            LOG.error("Exception while disposing BtLEQueue", ex);
//        }
    }

    /**
     * Adds a transaction to the end of the queue.
     *
     * @param transaction
     */
    public void add(Transaction transaction) {
        LOG.debug("about to add: " + transaction);
        if (!transaction.isEmpty()) {
            mTransactions.add(transaction);
            synchronized(mTransactionMonitor) {
                mTransactionMonitor.notify();
            }
        }
    }

    /**
     * Adds a serverTransaction to the end of the queue
     *
     * @param transaction
     */
    public void add(ServerTransaction transaction) {
        LOG.debug("about to add: " + transaction);
        if(!transaction.isEmpty()) {
            mServerTransactions.add(transaction);
            synchronized(mTransactionMonitor) {
                mTransactionMonitor.notify();
            }
        }
    }

    /**
     * Adds a transaction to the beginning of the queue.
     * Note that actions of the *currently executing* transaction
     * will still be executed before the given transaction.
     *
     * @param transaction
     */
    public void insert(Transaction transaction) {
        LOG.debug("about to insert: " + transaction);
        if (!transaction.isEmpty()) {
            List<Transaction> tail = new ArrayList<>(mTransactions.size() + 2);
            //mTransactions.drainTo(tail);
            for( Transaction t : mTransactions) {
                tail.add(t);
            }
            mTransactions.clear();
            mTransactions.add(transaction);
            mTransactions.addAll(tail);
            synchronized(mTransactionMonitor) {
                mTransactionMonitor.notify();
            }
        }
    }

    public void clear() {
        mTransactions.clear();
        mServerTransactions.clear();
        synchronized(mTransactionMonitor) {
            mTransactionMonitor.notify();
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            LOG.warn("BluetoothGatt is null => no services available.");
            return Collections.emptyList();
        }
        return mBluetoothGatt.getServices();
    }

    private boolean checkCorrectGattInstance(BluetoothGatt gatt, String where) {
        if (gatt != mBluetoothGatt && mBluetoothGatt != null) {
            LOG.info("Ignoring event from wrong BluetoothGatt instance: " + where + "; " + gatt);
            return false;
        }
        return true;
    }

    private boolean checkCorrectBluetoothDevice(BluetoothDevice device) {
        //BluetoothDevice clientDevice = mBluetoothAdapter.getRemoteDevice(mGbDevice.getAddress());

        if(!device.getAddress().equals(mGbDevice.getAddress())) { // != clientDevice && clientDevice != null) {
            LOG.info("Ignoring request from wrong Bluetooth device: " + device.getAddress());
            return false;
        }
        return true;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final class InternalGattCallback extends BluetoothGattCallback {
        private
        @Nullable
        GattCallback mTransactionGattCallback;
        private final GattCallback mExternalGattCallback;

        public InternalGattCallback(GattCallback externalGattCallback) {
            mExternalGattCallback = externalGattCallback;
        }

        public void setTransactionGattCallback(@Nullable GattCallback callback) {
            mTransactionGattCallback = callback;
        }

        private GattCallback getCallbackToUse() {
            if (mTransactionGattCallback != null) {
                return mTransactionGattCallback;
            }
            return mExternalGattCallback;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            LOG.debug("connection state change, newState: " + newState + getStatusString(status));

            synchronized (mGattMonitor) {
                if (mBluetoothGatt == null) {
                    mBluetoothGatt = gatt;
                }
            }

            if (!checkCorrectGattInstance(gatt, "connection state event")) {
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                LOG.warn("connection state event with error status " + status);
            }

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    LOG.info("Connected to GATT server.");
                    setDeviceConnectionState(State.CONNECTED);
                    // Attempts to discover services after successful connection.
                    List<BluetoothGattService> cachedServices = gatt.getServices();
                    if (cachedServices != null && cachedServices.size() > 0) {
                        LOG.info("Using cached services, skipping discovery");
                        onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                    } else {
                        LOG.info("Attempting to start service discovery");
                        // discover services in the main thread (appears to fix Samsung connection problems)
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (mBluetoothGatt != null) {
                                    mBluetoothGatt.discoverServices();
                                }
                            }
                        });
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    LOG.info("Disconnected from GATT server.");
                    handleDisconnected(status);
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    LOG.info("Connecting to GATT server...");
                    setDeviceConnectionState(State.CONNECTING);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (!checkCorrectGattInstance(gatt, "services discovered: " + getStatusString(status))) {
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (getCallbackToUse() != null) {
                    // only propagate the successful event
                    getCallbackToUse().onServicesDiscovered(gatt);
                }
            } else {
                LOG.warn("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LOG.debug("characteristic write: " + characteristic.getUuid() + getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "characteristic write")) {
                return;
            }
            if (getCallbackToUse() != null) {
                getCallbackToUse().onCharacteristicWrite(gatt, characteristic, status);
            }
            checkWaitingCharacteristic(characteristic, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            LOG.debug("characteristic read: " + characteristic.getUuid() + getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "characteristic read")) {
                return;
            }
            if (getCallbackToUse() != null) {
                try {
                    getCallbackToUse().onCharacteristicRead(gatt, characteristic, status);
                } catch (Throwable ex) {
                    LOG.error("onCharacteristicRead: " + ex.getMessage(), ex);
                }
            }
            checkWaitingCharacteristic(characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            LOG.debug("descriptor read: " + descriptor.getUuid() + getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "descriptor read")) {
                return;
            }
            if (getCallbackToUse() != null) {
                try {
                    getCallbackToUse().onDescriptorRead(gatt, descriptor, status);
                } catch (Throwable ex) {
                    LOG.error("onDescriptorRead: " + ex.getMessage(), ex);
                }
            }
            checkWaitingCharacteristic(descriptor.getCharacteristic(), status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            LOG.debug("descriptor write: " + descriptor.getUuid() + getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "descriptor write")) {
                return;
            }
            if (getCallbackToUse() != null) {
                try {
                    getCallbackToUse().onDescriptorWrite(gatt, descriptor, status);
                } catch (Throwable ex) {
                    LOG.error("onDescriptorWrite: " + ex.getMessage(), ex);
                }
            }
            checkWaitingCharacteristic(descriptor.getCharacteristic(), status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (LOG.isDebugEnabled()) {
                String content = Logging.formatBytes(characteristic.getValue());
                LOG.debug("characteristic changed: " + characteristic.getUuid() + " value: " + content);
            }
            if (!checkCorrectGattInstance(gatt, "characteristic changed")) {
                return;
            }
            if (getCallbackToUse() != null) {
                try {
                    getCallbackToUse().onCharacteristicChanged(gatt, characteristic);
                } catch (Throwable ex) {
                    LOG.error("onCharaceristicChanged: " + ex.getMessage(), ex);
                }
            } else {
                LOG.info("No gattcallback registered, ignoring characteristic change");
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            LOG.debug("remote rssi: " + rssi + getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "remote rssi")) {
                return;
            }
            if (getCallbackToUse() != null) {
                try {
                    getCallbackToUse().onReadRemoteRssi(gatt, rssi, status);
                } catch (Throwable ex) {
                    LOG.error("onReadRemoteRssi: " + ex.getMessage(), ex);
                }
            }
        }

        private void checkWaitingCharacteristic(BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (characteristic != null) {
                    LOG.debug("failed btle action, aborting transaction: " + characteristic.getUuid() + getStatusString(status));
                }
                mAbortTransaction = true;
            }
            if (characteristic != null && BtLEQueue.this.mWaitCharacteristic != null && characteristic.getUuid().equals(BtLEQueue.this.mWaitCharacteristic.getUuid())) {
                if (mWaitForActionResultLatch != null) {
                    mWaitForActionResultLatch.countDown();
                }
            } else {
                if (BtLEQueue.this.mWaitCharacteristic != null) {
                    LOG.error("checkWaitingCharacteristic: mismatched characteristic received: " + ((characteristic != null && characteristic.getUuid() != null) ? characteristic.getUuid().toString() : "(null)"));
                }
            }
        }

        private String getStatusString(int status) {
            return status == BluetoothGatt.GATT_SUCCESS ? " (success)" : " (failed: " + status + ")";
        }

        public void reset() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("internal gatt callback set to null");
            }
            mTransactionGattCallback = null;
        }
    }

    // Implements callback methods for GATT server events that the app cares about.  For example,
    // connection change and read/write requests.
    private final class InternalGattServerCallback extends BluetoothGattServerCallback {
        private
        @Nullable
        GattServerCallback mTransactionGattCallback;
        private final GattServerCallback mExternalGattServerCallback;

        public InternalGattServerCallback(GattServerCallback externalGattServerCallback) {
            mExternalGattServerCallback = externalGattServerCallback;
        }

        public void setTransactionGattCallback(@Nullable GattServerCallback callback) {
            mTransactionGattCallback = callback;
        }

        private GattServerCallback getCallbackToUse() {
            if (mTransactionGattCallback != null) {
                return mTransactionGattCallback;
            }
            return mExternalGattServerCallback;
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            LOG.debug("gatt server connection state change, newState: " + newState + getStatusString(status));

            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                LOG.warn("connection state event with error status " + status);
            }
        }

        private String getStatusString(int status) {
            return status == BluetoothGatt.GATT_SUCCESS ? " (success)" : " (failed: " + status + ")";
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }
            LOG.debug("characterstic read request: " + device.getAddress() + " characteristic: " + characteristic.getUuid());
            if (getCallbackToUse() != null) {
                getCallbackToUse().onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }
            LOG.debug("characteristic write request: " + device.getAddress() + " characteristic: " + characteristic.getUuid());
            if (getCallbackToUse() != null) {
                getCallbackToUse().onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }
            LOG.debug("onDescriptorReadRequest: " + device.getAddress());
            if(getCallbackToUse() != null) {
                getCallbackToUse().onDescriptorReadRequest(device, requestId, offset, descriptor);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }
            LOG.debug("onDescriptorWriteRequest: " + device.getAddress());
            if(getCallbackToUse() != null) {
                getCallbackToUse().onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }
        }
    }

}

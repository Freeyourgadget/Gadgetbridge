/*  Copyright (C) 2022-2024 Damien Gaignon, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.btbr;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public final class BtBRQueue {
    private static final Logger LOG = LoggerFactory.getLogger(BtBRQueue.class);

    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private final GBDevice mGbDevice;
    private final SocketCallback mCallback;
    private final UUID mService;

    private final BlockingQueue<AbstractTransaction> mTransactions = new LinkedBlockingQueue<>();
    private volatile boolean mDisposed;
    private volatile boolean mCrashed;

    private final Context mContext;
    private CountDownLatch mConnectionLatch;
    private CountDownLatch mAvailableData;
    private final int mBufferSize;

    private Thread writeThread = new Thread("Write Thread") {
        @Override
        public void run() {
            LOG.debug("Started write thread for {} (address {})", mGbDevice.getName(), mGbDevice.getAddress());
            
            while (!mDisposed && !mCrashed) {
                try {
                    AbstractTransaction qTransaction = mTransactions.take();
                    if (!isConnected()) {
                        LOG.debug("Not connected, waiting for connection...");
                        setDeviceConnectionState(GBDevice.State.NOT_CONNECTED);
                        // wait until the connection succeeds before running the actions
                        // Note that no automatic connection is performed. This has to be triggered
                        // on the outside typically by the DeviceSupport. The reason is that
                        // devices have different kinds of initializations and this class has no
                        // idea about them.
                        mConnectionLatch = new CountDownLatch(1);
                        mConnectionLatch.await();
                        mConnectionLatch = null;
                    }
                    LOG.info("Ready for a new message exchange.");
                    Transaction transaction = (Transaction)qTransaction;
                    for (BtBRAction action : transaction.getActions()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("About to run action: " + action);
                        }
                        if (action.run(mBtSocket)) {
                            LOG.debug("Action ok: " + action);
                        } else {
                            LOG.error("Action returned false: " + action);
                            break;
                        }
                    }
                }  catch (InterruptedException ignored) {
                    mConnectionLatch = null;
                    LOG.debug("Thread interrupted");
                } catch (Throwable ex) {
                    LOG.error("IO Write Thread died: " + ex.getMessage(), ex);
                    mCrashed = true;
                    mConnectionLatch = null;
                }
            }
        }
    };

    private Thread readThread = new Thread("Read Thread") {
        @Override
        public void run() {
            byte[] buffer = new byte[mBufferSize];
            int nRead;

            LOG.debug("Read thread started, entering loop");

            while (!mDisposed && !mCrashed) {
                try {
                    if (!isConnected()) {
                        LOG.debug("not connected, waiting for connection...");
                        // wait until the connection succeeds before running the actions
                        // Note that no automatic connection is performed. This has to be triggered
                        // on the outside typically by the DeviceSupport. The reason is that
                        // devices have different kinds of initializations and this class has no
                        // idea about them.
                        mConnectionLatch = new CountDownLatch(1);
                        mConnectionLatch.await();
                        mConnectionLatch = null;
                    }

                    if (mAvailableData != null) {
                        if (mBtSocket.getInputStream().available() == 0) {
                            mAvailableData.countDown();
                        }
                    }

                    nRead = mBtSocket.getInputStream().read(buffer);

                    // safety measure
                    if (nRead == -1) {
                        throw new IOException("End of stream");
                    }
                } catch (InterruptedException ignored) {
                    LOG.debug("Thread interrupted");
                    mConnectionLatch = null;
                    continue;
                } catch (Throwable ex) {
                    if (mAvailableData == null) {
                        LOG.error("IO read thread died: " + ex.getMessage(), ex);
                        mCrashed = true;
                    }

                    mConnectionLatch = null;
                    continue;
                }

                LOG.debug("Received {} bytes: {}", nRead, GB.hexdump(buffer, 0, nRead));

                try {
                    mCallback.onSocketRead(Arrays.copyOf(buffer, nRead));
                } catch (Throwable ex) {
                    LOG.error("Failed to process received bytes in onSocketRead callback: ", ex);
                }
            }

            LOG.debug("Exited read thread loop");
        }
    };

    public BtBRQueue(BluetoothAdapter btAdapter, GBDevice gbDevice, Context context, SocketCallback socketCallback, UUID supportedService, int bufferSize) {
        mBtAdapter = btAdapter;
        mGbDevice = gbDevice;
        mContext = context;
        mCallback = socketCallback;
        mService = supportedService;
        mBufferSize = bufferSize;
    }

    /**
     * Connects to the given remote device. Note that this does not perform any device
     * specific initialization. This should be done in the specific {@link DeviceSupport}
     * class.
     *
     * @return <code>true</code> whether the connection attempt was successfully triggered and <code>false</code> if that failed or if there is already a connection
     */
    @SuppressLint("MissingPermission")
    public boolean connect() {
        if (isConnected()) {
            LOG.warn("Ignoring connect() because already connected.");
            return false;
        }

        LOG.info("Attempting to connect to {} ({})", mGbDevice.getName(), mGbDevice.getAddress());

        // stop discovery before connection is made
        mBtAdapter.cancelDiscovery();

        // revert to original state upon exception
        GBDevice.State originalState = mGbDevice.getState();
        setDeviceConnectionState(GBDevice.State.CONNECTING);

        try {
            BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(mGbDevice.getAddress());
            mBtSocket = btDevice.createRfcommSocketToServiceRecord(mService);

            LOG.debug("RFCOMM socket created, connecting");

            // TODO this call is blocking, which makes this method preferably called from a background thread
            mBtSocket.connect();

            LOG.info("Connected to RFCOMM socket for {}", mGbDevice.getName());
            setDeviceConnectionState(GBDevice.State.CONNECTED);

            // update thread names to show device names in logs
            readThread.setName(String.format(Locale.ENGLISH,
                    "Read Thread for %s", mGbDevice.getName()));
            writeThread.setName(String.format(Locale.ENGLISH,
                    "Write Thread for %s", mGbDevice.getName()));

            // now that connect has been created, start the threads
            readThread.start();
            writeThread.start();
        } catch (IOException e) {
            LOG.error("Unable to connect to RFCOMM endpoint: ", e);
            setDeviceConnectionState(originalState);
            mBtSocket = null;
            return false;
        }

        onConnectionEstablished();
        return true;
    }

    protected void onConnectionEstablished() {
        mCallback.onConnectionEstablished();
    }

    public void disconnect() {
        if (mBtSocket != null) {
            try {
                mAvailableData = new CountDownLatch(1);

                if (!mAvailableData.await(1, TimeUnit.SECONDS)) {
                    LOG.warn("disconnect(): Latch timeout reached while waiting for incoming data");
                }

                mAvailableData = null;
                mBtSocket.close();
            } catch (IOException | InterruptedException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    /**
     * Check whether a connection to the device exists and whether a socket connection has been
     * initialized and connected
     * @return true if the Bluetooth device is connected and the socket is ready, false otherwise
     */
    private boolean isConnected() {
        return mGbDevice.isConnected() &&
                mBtSocket != null &&
                mBtSocket.isConnected();
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
        }
    }

    private void setDeviceConnectionState(GBDevice.State newState) {
        LOG.debug("New device connection state: " + newState);
        mGbDevice.setState(newState);
        mGbDevice.sendDeviceUpdateIntent(mContext, GBDevice.DeviceUpdateSubject.CONNECTION_STATE);
    }

    public void dispose() {
        if (mDisposed) {
            return;
        }

        mDisposed = true;
        disconnect();
        writeThread.interrupt();
        writeThread = null;
        readThread.interrupt();
        readThread = null;
        mTransactions.clear();
    }
}

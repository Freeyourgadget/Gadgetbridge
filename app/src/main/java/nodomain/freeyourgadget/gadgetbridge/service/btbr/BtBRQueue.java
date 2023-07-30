/*  Copyright (C) 2022 Damien Gaignon
*
*    This file is part of Gadgetbridge.
*
*    Gadgetbridge is free software: you can redistribute it and/or modify
*    it under the terms of the GNU Affero General Public License as published
*    by the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    Gadgetbridge is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU Affero General Public License for more details.
*
*    You should have received a copy of the GNU Affero General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package nodomain.freeyourgadget.gadgetbridge.service.btbr;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public final class BtBRQueue {
    private static final Logger LOG = LoggerFactory.getLogger(BtBRQueue.class);

    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private GBDevice mGbDevice;
    private SocketCallback mCallback;
    private UUID mService;

    private final BlockingQueue<AbstractTransaction> mTransactions = new LinkedBlockingQueue<>();
    private volatile boolean mDisposed;
    private volatile boolean mCrashed;

    private Context mContext;
    private CountDownLatch mConnectionLatch;
    private CountDownLatch mAvailableData;
    private int mBufferSize;

    private Thread writeThread = new Thread("Gadgetbridge IO writeThread") {
        @Override
        public void run() {
            LOG.debug("Socket Write Thread started.");
            
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

    private Thread readThread = new Thread("Gadgetbridge IO readThread") {
        @Override
        public void run() {
            LOG.debug("Queue Read Thread started.");
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
                    byte[] data = new byte[mBufferSize];
                    int len = mBtSocket.getInputStream().read(data);
                    LOG.debug("Received data: " + StringUtils.bytesToHex(data));
                    mCallback.onSocketRead(Arrays.copyOf(data, len));
                }  catch (InterruptedException ignored) {
                    mConnectionLatch = null;
                    LOG.debug("Thread interrupted");
                } catch (Throwable ex) {
                    LOG.error("IO Read Thread died: " + ex.getMessage(), ex);
                    mCrashed = true;
                    mConnectionLatch = null;
                }
            }
        }
    };

    public BtBRQueue(BluetoothAdapter btAdapter, GBDevice gbDevice, Context context, SocketCallback socketCallback, UUID supportedService, int bufferSize) {
        mBtAdapter = btAdapter;
        mGbDevice = gbDevice;
        mContext = context;
        mCallback = socketCallback;
        mService = supportedService;
        mBufferSize = bufferSize;

        writeThread.start();
        readThread.start();
    }

    /**
     * Connects to the given remote device. Note that this does not perform any device
     * specific initialization. This should be done in the specific {@link DeviceSupport}
     * class.
     *
     * @return <code>true</code> whether the connection attempt was successfully triggered and <code>false</code> if that failed or if there is already a connection
     */

    protected boolean connect() {
        if (isConnected()) {
            LOG.warn("Ignoring connect() because already connected.");
            return false;
        }

        LOG.info("Attemping to connect to " + mGbDevice.getName());
        GBDevice.State originalState = mGbDevice.getState();
        setDeviceConnectionState(GBDevice.State.CONNECTING);

        try {
            BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(mGbDevice.getAddress());
            // UUID should be in a BluetoothSocket class and not in BluetoothSocketCharacteristic
            mBtSocket = btDevice.createRfcommSocketToServiceRecord(mService);
            mBtSocket.connect();
            if (mBtSocket.isConnected()) {
                setDeviceConnectionState(GBDevice.State.CONNECTED);
            } else {
                LOG.debug("Connection not established");
            }
            if (mConnectionLatch != null) {
                mConnectionLatch.countDown();
            }
        } catch (IOException e) {
            LOG.error("Server socket cannot be started.", e);
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
                mAvailableData.await();
                mAvailableData = null;
                mBtSocket.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            } catch (InterruptedException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    protected boolean isConnected() {
        return mGbDevice.isConnected();
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

    protected void setDeviceConnectionState(GBDevice.State newState) {
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
    }

}

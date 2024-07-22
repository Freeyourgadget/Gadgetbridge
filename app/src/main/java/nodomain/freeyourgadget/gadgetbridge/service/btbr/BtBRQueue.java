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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public final class BtBRQueue {
    private static final Logger LOG = LoggerFactory.getLogger(BtBRQueue.class);
    public static final int HANDLER_SUBJECT_CONNECT = 0;
    public static final int HANDLER_SUBJECT_PERFORM_TRANSACTION = 1;

    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private final GBDevice mGbDevice;
    private final SocketCallback mCallback;
    private final UUID mService;

    private volatile boolean mDisposed;

    private final Context mContext;
    private final int mBufferSize;

    private final Handler mWriteHandler;
    private final HandlerThread mWriteHandlerThread = new HandlerThread("Write Thread", Process.THREAD_PRIORITY_BACKGROUND);

    private Thread readThread = new Thread("Read Thread") {
        @Override
        public void run() {
            byte[] buffer = new byte[mBufferSize];
            int nRead;

            LOG.debug("Read thread started, entering loop");

            while (!mDisposed) {
                try {
                    nRead = mBtSocket.getInputStream().read(buffer);

                    // safety measure
                    if (nRead == -1) {
                        throw new IOException("End of stream");
                    }
                } catch (IOException ex) {
                    LOG.error("IO exception while reading message from socket, breaking out of read thread: ", ex);
                    break;
                }

                LOG.debug("Received {} bytes: {}", nRead, GB.hexdump(buffer, 0, nRead));

                try {
                    mCallback.onSocketRead(Arrays.copyOf(buffer, nRead));
                } catch (Throwable ex) {
                    LOG.error("Failed to process received bytes in onSocketRead callback: ", ex);
                }
            }

            LOG.debug("Exited read thread loop, disconnecting");
            GBApplication.deviceService(mGbDevice).disconnect();
        }
    };

    public BtBRQueue(BluetoothAdapter btAdapter, GBDevice gbDevice, Context context, SocketCallback socketCallback, UUID supportedService, int bufferSize) {
        mBtAdapter = btAdapter;
        mGbDevice = gbDevice;
        mContext = context;
        mCallback = socketCallback;
        mService = supportedService;
        mBufferSize = bufferSize;

        mWriteHandlerThread.start();

        LOG.debug("Write handler thread is prepared, creating write handler");
        mWriteHandler = new Handler(mWriteHandlerThread.getLooper()) {
            @SuppressLint("MissingPermission")
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case HANDLER_SUBJECT_CONNECT: {
                        try {
                            mBtSocket.connect();

                            LOG.info("Connected to RFCOMM socket for {}", mGbDevice.getName());
                            setDeviceConnectionState(GBDevice.State.CONNECTED);

                            // update thread names to show device names in logs
                            readThread.setName(String.format(Locale.ENGLISH,
                                    "Read Thread for %s", mGbDevice.getName()));
                            mWriteHandlerThread.setName(String.format(Locale.ENGLISH,
                                    "Write Thread for %s", mGbDevice.getName()));

                            // now that connect has been created, start the threads
                            readThread.start();
                            onConnectionEstablished();
                        } catch (IOException e) {
                            LOG.error("IO exception while establishing socket connection: ", e);
                            setDeviceConnectionState(GBDevice.State.NOT_CONNECTED);
                        }

                        return;
                    }
                    case HANDLER_SUBJECT_PERFORM_TRANSACTION: {
                        try {
                            if (!isConnected()) {
                                LOG.debug("Not connected, updating device state to WAITING_FOR_RECONNECT");
                                setDeviceConnectionState(GBDevice.State.WAITING_FOR_RECONNECT);
                                return;
                            }

                            if (!(msg.obj instanceof Transaction)) {
                                LOG.error("msg.obj is not an instance of Transaction");
                                return;
                            }

                            Transaction transaction = (Transaction) msg.obj;

                            for (BtBRAction action : transaction.getActions()) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("About to run action: {}", action);
                                }

                                if (action.run(mBtSocket)) {
                                    LOG.debug("Action ok: {}", action);
                                } else {
                                    LOG.error("Action returned false, cancelling further actions in transaction: {}", action);
                                    break;
                                }
                            }
                        } catch (Throwable ex) {
                            LOG.error("IO Write Thread died: " + ex.getMessage(), ex);
                        }

                        return;
                    }
                }

                LOG.warn("Unhandled write handler message {}", msg.what);
            }
        };
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
        } catch (IOException e) {
            LOG.error("Unable to connect to RFCOMM endpoint: ", e);
            setDeviceConnectionState(originalState);
            mBtSocket = null;
            return false;
        }

        LOG.debug("Socket created, connecting in handler");
        mWriteHandler.sendMessageAtFrontOfQueue(mWriteHandler.obtainMessage(HANDLER_SUBJECT_CONNECT));
        return true;
    }

    protected void onConnectionEstablished() {
        mCallback.onConnectionEstablished();
    }

    public void disconnect() {
        if (mWriteHandlerThread.isAlive()) {
            mWriteHandlerThread.quit();
        }

        if (mBtSocket != null && mBtSocket.isConnected()) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                LOG.error("IO exception while closing socket in disconnect(): ", e);
            }
        }

        mBtSocket = null;
        setDeviceConnectionState(GBDevice.State.NOT_CONNECTED);
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
     * Add a finalized {@link Transaction} to the write handler's queue
     *
     * @param transaction The transaction to be run in the handler thread's looper
     */
    public void add(Transaction transaction) {
        LOG.debug("Adding transaction to looper message queue: {}", transaction);

        if (!transaction.isEmpty()) {
            mWriteHandler.obtainMessage(HANDLER_SUBJECT_PERFORM_TRANSACTION, transaction).sendToTarget();
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

        if (readThread != null && readThread.isAlive()) {
            readThread.interrupt();
            readThread = null;
        }
    }
}

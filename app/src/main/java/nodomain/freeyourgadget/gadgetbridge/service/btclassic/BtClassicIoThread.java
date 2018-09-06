/*  Copyright (C) 2016-2018 Carsten Pfeiffer, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.btclassic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class BtClassicIoThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(BtClassicIoThread.class);

    private final GBDeviceProtocol mProtocol;
    private final AbstractSerialDeviceSupport mDeviceSupport;


    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private InputStream mInStream = null;
    private OutputStream mOutStream = null;
    private boolean mQuit = false;

    @Override
    public void quit() {
        mQuit = true;
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    private boolean mIsConnected = false;


    public BtClassicIoThread(GBDevice gbDevice, Context context, GBDeviceProtocol deviceProtocol, AbstractSerialDeviceSupport deviceSupport, BluetoothAdapter btAdapter) {
        super(gbDevice, context);
        mProtocol = deviceProtocol;
        mDeviceSupport = deviceSupport;
        mBtAdapter = btAdapter;
    }

    @Override
    public synchronized void write(byte[] bytes) {
        if (null == bytes)
            return;
        if (mOutStream == null) {
            LOG.error("mOutStream is null");
            return;
        }
        LOG.debug("writing:" + GB.hexdump(bytes, 0, bytes.length));
        try {
            mOutStream.write(bytes);
            mOutStream.flush();
        } catch (IOException e) {
            LOG.error("Error writing.", e);
        }
    }

    @Override
    public void run() {
        mIsConnected = connect();
        if (!mIsConnected) {
            setUpdateState(GBDevice.State.NOT_CONNECTED);
            return;
        }
        mQuit = false;

        while (!mQuit) {
            LOG.info("Ready for a new message exchange.");

            try {
                GBDeviceEvent deviceEvents[] = mProtocol.decodeResponse(parseIncoming(mInStream));
                if (deviceEvents == null) {
                    LOG.info("unhandled message");
                } else {
                    for (GBDeviceEvent deviceEvent : deviceEvents) {
                        if (deviceEvent == null) {
                            continue;
                        }
                        mDeviceSupport.evaluateGBDeviceEvent(deviceEvent);
                    }
                }
            } catch (SocketTimeoutException ignore) {
                LOG.debug("socket timeout, we can't help but ignore this");
            } catch (IOException e) {
                LOG.info(e.getMessage());
                mIsConnected = false;
                mBtSocket = null;
                mInStream = null;
                mOutStream = null;
                LOG.info("Bluetooth socket closed, will quit IO Thread");
                break;
            }
        }

        mIsConnected = false;
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
            mBtSocket = null;
        }
        setUpdateState(GBDevice.State.NOT_CONNECTED);
    }

    @Override
    protected boolean connect() {
        GBDevice.State originalState = gbDevice.getState();
        setUpdateState(GBDevice.State.CONNECTING);

        try {
            BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(gbDevice.getAddress());
            ParcelUuid uuids[] = btDevice.getUuids();
            if (uuids == null) {
                LOG.warn("Device provided no UUIDs to connect to, giving up: " + gbDevice);
                return false;
            }
            for (ParcelUuid uuid : uuids) {
                LOG.info("found service UUID " + uuid);
            }
            mBtSocket = btDevice.createRfcommSocketToServiceRecord(getUuidToConnect(uuids));
            mBtSocket.connect();
            mInStream = mBtSocket.getInputStream();
            mOutStream = mBtSocket.getOutputStream();
            setUpdateState(GBDevice.State.CONNECTED);
        } catch (IOException e) {
            LOG.error("Server socket cannot be started.");
            //LOG.error(e.getMessage());
            setUpdateState(originalState);
            mInStream = null;
            mOutStream = null;
            mBtSocket = null;
            return false;
        }

        write(mProtocol.encodeSetTime());
        setUpdateState(GBDevice.State.INITIALIZED);

        return true;
    }

    /**
     * Returns the uuid to connect to.
     * Default implementation returns the first of the given uuids that were
     * read from the remote device.
     * @param uuids
     * @return
     */
    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return uuids[0].getUuid();
    }

    protected void setUpdateState(GBDevice.State state) {
        gbDevice.setState(state);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }

    /**
     * Returns an incoming message for consuming by the GBDeviceProtocol
     * @return
     * @throws IOException
     * @param inStream
     */
    protected abstract byte[] parseIncoming(InputStream inStream) throws IOException;
}

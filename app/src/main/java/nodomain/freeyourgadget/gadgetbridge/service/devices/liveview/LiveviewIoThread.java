/*  Copyright (C) 2016-2017 Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.liveview;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.ParcelUuid;

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
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class LiveviewIoThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(LiveviewIoThread.class);

    private static final UUID SERIAL = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final LiveviewProtocol mLiveviewProtocol;
    private final LiveviewSupport mLiveviewSupport;


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


    public LiveviewIoThread(GBDevice gbDevice, Context context, GBDeviceProtocol lvProtocol, LiveviewSupport lvSupport, BluetoothAdapter lvBtAdapter) {
        super(gbDevice, context);
        mLiveviewProtocol = (LiveviewProtocol) lvProtocol;
        mBtAdapter = lvBtAdapter;
        mLiveviewSupport = lvSupport;
    }

    @Override
    public synchronized void write(byte[] bytes) {
        if (null == bytes)
            return;
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
                GBDeviceEvent deviceEvents[] = mLiveviewProtocol.decodeResponse(parseIncoming());
                if (deviceEvents == null) {
                    LOG.info("unhandled message");
                } else {
                    for (GBDeviceEvent deviceEvent : deviceEvents) {
                        if (deviceEvent == null) {
                            continue;
                        }
                        mLiveviewSupport.evaluateGBDeviceEvent(deviceEvent);
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
                return false;
            }
            for (ParcelUuid uuid : uuids) {
                LOG.info("found service UUID " + uuid);
            }
            mBtSocket = btDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
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

        write(mLiveviewProtocol.encodeSetTime());
        setUpdateState(GBDevice.State.INITIALIZED);

        return true;
    }

    private void setUpdateState(GBDevice.State state) {
        gbDevice.setState(state);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }

    private byte[] parseIncoming() throws IOException {
        ByteArrayOutputStream msgStream = new ByteArrayOutputStream();

        boolean finished = false;
        ReaderState state = ReaderState.ID;
        byte[] incoming = new byte[1];

        while (!finished) {
            mInStream.read(incoming);
            msgStream.write(incoming);

            switch (state) {
                case ID:
                    state = ReaderState.HEADER_LEN;
                    incoming = new byte[1];
                    break;
                case HEADER_LEN:
                    int headerSize = 0xff & incoming[0];
                    if (headerSize < 0)
                        throw new IOException();
                    state = ReaderState.HEADER;
                    incoming = new byte[headerSize];
                    break;
                case HEADER:
                    int payloadSize = getLastInt(msgStream);
                    if (payloadSize < 0 || payloadSize > 8000) //this will possibly be changed in the future
                        throw new IOException();
                    state = ReaderState.PAYLOAD;
                    incoming = new byte[payloadSize];
                    break;
                case PAYLOAD: //read is blocking, if we are here we have all the data
                    finished = true;
                    break;
            }
        }
        byte[] msgArray = msgStream.toByteArray();
        LOG.debug("received: " + GB.hexdump(msgArray, 0, msgArray.length));
        return msgArray;
    }


    /**
     * Enumeration containing the possible internal status of the reader.
     */
    private enum ReaderState {
        ID, HEADER_LEN, HEADER, PAYLOAD;
    }

    private int getLastInt(ByteArrayOutputStream stream) {
        byte[] array = stream.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(array, array.length - 4, 4);
        buffer.order(LiveviewConstants.BYTE_ORDER);
        return buffer.getInt();
    }
}

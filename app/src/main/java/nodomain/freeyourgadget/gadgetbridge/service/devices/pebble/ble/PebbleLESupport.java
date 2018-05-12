/*  Copyright (C) 2016-2018 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class PebbleLESupport {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleLESupport.class);
    private final BluetoothDevice mBtDevice;
    private PipeReader mPipeReader;
    private PebbleGATTServer mPebbleGATTServer;
    private PebbleGATTClient mPebbleGATTClient;
    private PipedInputStream mPipedInputStream;
    private PipedOutputStream mPipedOutputStream;
    private int mMTU = 20;
    private int mMTULimit = Integer.MAX_VALUE;
    public boolean clientOnly = false; // currently experimental, and only possible for Pebble 2
    private boolean mIsConnected = false;
    private CountDownLatch mPPAck;
    private HandlerThread mWriteHandlerThread;
    private Handler mWriteHandler;

    public PebbleLESupport(Context context, final BluetoothDevice btDevice, PipedInputStream pipedInputStream, PipedOutputStream pipedOutputStream) throws IOException {
        mBtDevice = btDevice;
        mPipedInputStream = new PipedInputStream();
        mPipedOutputStream = new PipedOutputStream();
        try {
            pipedOutputStream.connect(mPipedInputStream);
            pipedInputStream.connect(mPipedOutputStream);
        } catch (IOException e) {
            LOG.warn("could not connect input stream");
        }

        mWriteHandlerThread = new HandlerThread("write handler thread");
        mWriteHandlerThread.start();
        mWriteHandler = new Handler(mWriteHandlerThread.getLooper());

        mMTULimit = GBApplication.getPrefs().getInt("pebble_mtu_limit", 512);
        mMTULimit = Math.max(mMTULimit, 20);
        mMTULimit = Math.min(mMTULimit, 512);

        clientOnly = GBApplication.getPrefs().getBoolean("pebble_gatt_clientonly", false);

        if (!clientOnly) {
            mPebbleGATTServer = new PebbleGATTServer(this, context, mBtDevice);
        }
        if (clientOnly || mPebbleGATTServer.initialize()) {
            mPebbleGATTClient = new PebbleGATTClient(this, context, mBtDevice);
            try {
                synchronized (this) {
                    wait(30000);
                    if (mIsConnected) {
                        return;
                    }
                }
            } catch (InterruptedException ignored) {
            }
        }
        this.close();
        throw new IOException("connection failed");
    }

    private void writeToPipedOutputStream(byte[] value, int offset, int count) {
        try {
            mPipedOutputStream.write(value, offset, count);
        } catch (IOException e) {
            LOG.warn("error writing to output stream", e);
        }
    }

    synchronized public void close() {
        destroyPipedInputReader();
        if (mPebbleGATTServer != null) {
            mPebbleGATTServer.close();
            mPebbleGATTServer = null;
        }
        if (mPebbleGATTClient != null) {
            mPebbleGATTClient.close();
            mPebbleGATTClient = null;
        }
        try {
            mPipedInputStream.close();
        } catch (IOException ignore) {
        }
        try {
            mPipedOutputStream.close();
        } catch (IOException ignore) {
        }
        if (mWriteHandlerThread != null) {
            mWriteHandlerThread.quit();
        }
    }

    private synchronized void createPipedInputReader() {
        if (mPipeReader == null) {
            mPipeReader = new PipeReader();
        }
        if (!mPipeReader.isAlive()) {
            mPipeReader.start();
        }
    }

    synchronized private void destroyPipedInputReader() {
        if (mPipeReader != null) {
            mPipeReader.interrupt();
            try {
                mPipeReader.join();
            } catch (InterruptedException e) {
                LOG.error(e.getMessage());
            }
            mPipeReader = null;
        }
    }

    void setMTU(int mtu) {
        mMTU = Math.min(mtu, mMTULimit);
    }

    public void handlePPoGATTPacket(byte[] value) {
        if (!mIsConnected) {
            mIsConnected = true;
            synchronized (this) {
                this.notify();
            }
        }
        //LOG.info("write request: offset = " + offset + " value = " + GB.hexdump(value, 0, -1));
        int header = value[0] & 0xff;
        int command = header & 7;
        int serial = header >> 3;
        if (command == 0x01) {
            LOG.info("got ACK for serial = " + serial);
            if (!clientOnly) {
                if (mPPAck != null) {
                    mPPAck.countDown();
                } else {
                    LOG.warn("mPPAck countdownlatch is not present but it probably should");
                }
            }
        }
        if (command == 0x02) { // some request?
            LOG.info("got command 0x02");
            if (value.length > 1) {
                sendDataToPebble(new byte[]{0x03, 0x19, 0x19}); // no we don't know what that means
                createPipedInputReader(); // FIXME: maybe not here
            } else {
                sendDataToPebble(new byte[]{0x03}); // no we don't know what that means
            }
        } else if (command == 0) { // normal package
            LOG.info("got PPoGATT package serial = " + serial + " sending ACK");

            sendAckToPebble(serial);

            writeToPipedOutputStream(value, 1, value.length - 1);
        }
    }

    private void sendAckToPebble(int serial) {
        sendDataToPebble(new byte[]{(byte) (((serial << 3) | 1) & 0xff)});
    }

    private synchronized void sendDataToPebble(final byte[] bytes) {
        if (mPebbleGATTServer != null) {
            mPebbleGATTServer.sendDataToPebble(bytes);
        } else {
            // For now only in experimental client only code
            mWriteHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPebbleGATTClient.sendDataToPebble(bytes);
                }
            });
        }
    }

    private class PipeReader extends Thread {
        int mmSequence = 0;

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int bytesRead;
            while (true) {
                try {
                    // this code is very similar to iothread, that is bad
                    // because we are the ones who prepared the buffer, there should be no
                    // need to do crazy stuff just to find out the PP boundaries again.
                    bytesRead = mPipedInputStream.read(buf, 0, 4);
                    while (bytesRead < 4) {
                        bytesRead += mPipedInputStream.read(buf, bytesRead, 4 - bytesRead);
                    }

                    int length = (buf[0] & 0xff) << 8 | (buf[1] & 0xff);
                    bytesRead = mPipedInputStream.read(buf, 4, length);

                    while (bytesRead < length) {
                        bytesRead += mPipedInputStream.read(buf, bytesRead + 4, length - bytesRead);
                    }


                    int payloadToSend = bytesRead + 4;
                    int srcPos = 0;
                    if (!clientOnly) {
                        mPPAck = new CountDownLatch(1);
                    }
                    while (payloadToSend > 0) {
                        int chunkSize = (payloadToSend < (mMTU - 4)) ? payloadToSend : mMTU - 4;
                        byte[] outBuf = new byte[chunkSize + 1];
                        outBuf[0] = (byte) ((mmSequence++ << 3) & 0xff);
                        System.arraycopy(buf, srcPos, outBuf, 1, chunkSize);
                        sendDataToPebble(outBuf);
                        srcPos += chunkSize;
                        payloadToSend -= chunkSize;
                    }
                    if (!clientOnly) {
                        mPPAck.await();
                        mPPAck = null;
                    }
                } catch (IOException | InterruptedException e) {
                    LOG.info(e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            LOG.info("Pipereader thread shut down");
        }

        @Override
        public void interrupt() {
            super.interrupt();
            try {
                LOG.info("closing piped inputstream");
                mPipedInputStream.close();
            } catch (IOException ignore) {
            }
        }
    }

    boolean isExpectedDevice(BluetoothDevice device) {
        if (!device.getAddress().equals(mBtDevice.getAddress())) {
            LOG.info("unhandled device: " + device.getAddress() + " , ignoring, will only talk to " + mBtDevice.getAddress());
            return false;
        }
        return true;
    }
}


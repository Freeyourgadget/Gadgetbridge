package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

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
    boolean mIsConnected = false;
    public CountDownLatch mPPAck;

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
        mMTULimit = GBApplication.getPrefs().getInt("pebble_mtu_limit", 512);
        mMTULimit = Math.max(mMTULimit, 20);
        mMTULimit = Math.min(mMTULimit, 512);

        mPebbleGATTServer = new PebbleGATTServer(this, context, mBtDevice);
        if (mPebbleGATTServer.initialize()) {
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

    void writeToPipedOutputStream(byte[] value, int offset, int count) {
        try {
            mPipedOutputStream.write(value, offset, count);
        } catch (IOException e) {
            LOG.warn("error writing to output stream");
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
    }

    synchronized void createPipedInputReader() {
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
                    mPPAck = new CountDownLatch(1);
                    while (payloadToSend > 0) {
                        int chunkSize = (payloadToSend < (mMTU - 4)) ? payloadToSend : mMTU - 4;
                        byte[] outBuf = new byte[chunkSize + 1];
                        outBuf[0] = (byte) ((mmSequence++ << 3) & 0xff);
                        System.arraycopy(buf, srcPos, outBuf, 1, chunkSize);
                        mPebbleGATTServer.sendDataToPebble(outBuf);
                        srcPos += chunkSize;
                        payloadToSend -= chunkSize;
                    }

                    mPPAck.await();
                    mPPAck = null;

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


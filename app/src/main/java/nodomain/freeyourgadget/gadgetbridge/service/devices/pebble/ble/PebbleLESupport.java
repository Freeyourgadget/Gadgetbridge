package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class PebbleLESupport {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleLESupport.class);
    private final BluetoothDevice mBtDevice;
    private PipeReader mPipeReader;
    private PebbleGATTServer mPebbleGATTServer;
    private PebbleGATTClient mPebbleGATTClient;
    private PipedInputStream mPipedInputStream;
    private PipedOutputStream mPipedOutputStream;
    private int mMTU = 20;

    public PebbleLESupport(Context context, final BluetoothDevice btDevice, PipedInputStream pipedInputStream, PipedOutputStream pipedOutputStream) {
        mBtDevice = btDevice;
        mPipedInputStream = new PipedInputStream();
        mPipedOutputStream = new PipedOutputStream();
        try {
            pipedOutputStream.connect(mPipedInputStream);
            pipedInputStream.connect(mPipedOutputStream);
        } catch (IOException e) {
            LOG.warn("could not connect input stream");
        }

        mPebbleGATTServer = new PebbleGATTServer(this, context, mBtDevice);
        mPebbleGATTServer.initialize();

        mPebbleGATTClient = new PebbleGATTClient(this, context, mBtDevice);
        mPebbleGATTClient.initialize();
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
        try {
            mPipedInputStream.close();
        } catch (IOException ignore) {
        }
    }

    void setMTU(int mtu) {
        mMTU = mtu;
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
                    while (payloadToSend > 0) {
                        int chunkSize = (payloadToSend < (mMTU - 4)) ? payloadToSend : mMTU - 4;
                        byte[] outBuf = new byte[chunkSize + 1];
                        outBuf[0] = (byte) ((mmSequence++ << 3) & 0xff);
                        System.arraycopy(buf, srcPos, outBuf, 1, chunkSize);
                        mPebbleGATTServer.sendDataToPebble(outBuf);
                        srcPos += chunkSize;
                        payloadToSend -= chunkSize;
                    }

                    Thread.sleep(500); // FIXME ugly wait 0.5s after each pebble package send to the pebble (we do not wait for the GATT chunks)
                } catch (InterruptedIOException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException ignore) {
                }
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            try {
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


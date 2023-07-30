/*  Copyright (C) 2022 Damien Gaignon

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
package nodomain.freeyourgadget.gadgetbridge.service.btbr;

import org.slf4j.Logger;

import android.location.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.CheckInitializedAction;

/**
 * Abstract base class for devices connected through a serial protocol, like RFCOMM BT or TCP socket.
 * <p/>
 * The connection to the device and all communication is made with a generic {@link BtClassicIo}.
 * Messages to the device are encoded
 * sent via {@link BtClassicIo}.
 *
 * @see BtClassicIo
 */
public abstract class AbstractBTBRDeviceSupport extends AbstractDeviceSupport implements SocketCallback {
    private BtBRQueue mQueue;
    private UUID mSupportedService = null;
    private int mBufferSize = 1024;
    private Logger logger;

    public AbstractBTBRDeviceSupport(Logger logger) {
        this.logger = logger;
        if (logger == null) {
            throw new IllegalArgumentException("logger must not be null");
        }
    }

    @Override
    public boolean connect() {
        if (mQueue == null) {
            mQueue = new BtBRQueue(getBluetoothAdapter(), getDevice(), getContext(), this, getSupportedService(), getBufferSize());
        }
        return mQueue.connect();
    }

    /**
     * Subclasses should populate the given builder to initialize the device (if necessary).
     *
     * @return the same builder as passed as the argument
     */
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        return builder;
    }

    @Override
    public void dispose() {
        if (mQueue != null) {
            mQueue.dispose();
            mQueue = null;
        }
    }

    public TransactionBuilder createTransactionBuilder(String taskName) {
        return new TransactionBuilder(taskName);
    }

    /**
     * Ensures that the device is connected and (only then) performs the actions of the given
     * transaction builder.
     *
     * In contrast to {@link #performInitialized(String)}, no initialization sequence is performed
     * with the device, only the actions of the given builder are executed.
     * @param transaction
     * @throws IOException
     * @see {@link #performInitialized(String)}
     */
    public void performConnected(Transaction transaction) throws IOException {
        if (!isConnected()) {
            if (!connect()) {
                throw new IOException("2: Unable to connect to device: " + getDevice());
            }
        }
        getQueue().add(transaction);
    }

    public BtBRQueue getQueue() {
        return mQueue;
    }

    /**
     * Subclasses should call this method to add services they support.
     * Only supported services will be queried for characteristics.
     *
     * @param aSupportedService
     */
    protected void addSupportedService(UUID aSupportedService) {
        mSupportedService = aSupportedService;
    }

    protected UUID getSupportedService() {
        return mSupportedService;
    }

    protected void setBufferSize(int bufferSize) {
        mBufferSize = bufferSize;
    }

    protected int getBufferSize() {
        return mBufferSize;
    }

    /**
     * Utility method that may be used to log incoming messages when we don't know how to deal with them yet.
     *
     * @param value
     */
    public void logMessageContent(byte[] value) {
        logger.info("RECEIVED DATA WITH LENGTH: " + ((value != null) ? value.length : "(null)"));
        Logging.logBytes(logger, value);
    }

    public void onConnectionEstablished() {
       initializeDevice(createTransactionBuilder("Initializing device")).queue(getQueue());
    }

    @Override
    public void onSetFmFrequency(float frequency) {}

    @Override
    public void onSetLedColor(int color) {}

    @Override
    public void onSetGpsLocation(Location location) {}

    @Override
    public void onSetWorldClocks(ArrayList<? extends WorldClock> clocks) {}

    @Override
    public void onPowerOff() {}

    @Override
    public void onSetPhoneVolume(final float volume) {}

    @Override
    public void onSetReminders(ArrayList<? extends Reminder> reminders) {}

}

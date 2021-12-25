/*  Copyright (C) 2020-2021 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests;

import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

// Ripped from nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request

/**
 * Basic request for operations with Lefun devices
 */
public abstract class Request extends AbstractBTLEOperation<LefunDeviceSupport> {
    protected TransactionBuilder builder;
    protected boolean removeAfterHandling = true;
    private Logger logger = (Logger) LoggerFactory.getLogger(getName());

    /**
     * Instantiates Request
     *
     * @param support the device support
     * @param builder the transaction builder to use
     */
    protected Request(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support);
        this.builder = builder;
    }

    /**
     * Gets the transaction builder
     *
     * @return the transaction builder
     */
    public TransactionBuilder getTransactionBuilder() {
        return builder;
    }

    @Override
    protected void doPerform() throws IOException {
        BluetoothGattCharacteristic characteristic = getSupport()
                .getCharacteristic(LefunConstants.UUID_CHARACTERISTIC_LEFUN_WRITE);
        builder.write(characteristic, createRequest());
        if (isSelfQueue())
            getSupport().performConnected(builder.getTransaction());
    }

    /**
     * When implemented in a subclass, provides the request bytes to send to the device
     *
     * @return the request bytes
     */
    public abstract byte[] createRequest();

    /**
     * When overridden in a subclass, handles the response to the current command
     *
     * @param data the response data
     */
    public void handleResponse(byte[] data) {
        operationStatus = OperationStatus.FINISHED;
    }

    /**
     * Gets the class name of this instance
     *
     * @return the class name
     */
    public String getName() {
        Class thisClass = getClass();
        while (thisClass.isAnonymousClass()) thisClass = thisClass.getSuperclass();
        return thisClass.getSimpleName();
    }

    /**
     * Logs a debug message
     *
     * @param message the message to log
     */
    protected void log(String message) {
        logger.debug(message);
    }

    /**
     * When implemented in a subclass, returns the command ID associated with the current request
     *
     * @return the command ID
     */
    public abstract int getCommandId();

    /**
     * Gets whether the request will queue itself
     *
     * @return whether the request is self-queuing
     */
    public boolean isSelfQueue() {
        return false;
    }

    /**
     * Gets whether the request expects a response
     *
     * @return whether the request expects a response
     */
    public boolean expectsResponse() {
        return true;
    }

    /**
     * Gets whether the response should be removed from in progress requests list after handling
     *
     * @return whether the response should be removed after handling
     */
    public boolean shouldRemoveAfterHandling() {
        return removeAfterHandling;
    }

    /**
     * Reports an error to the user
     *
     * @param message the message to show
     */
    protected void reportFailure(String message) {
        GB.toast(getContext(), message, Toast.LENGTH_SHORT, GB.ERROR);
    }
}

/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti
    Copyright (C) 2020 Yukai Li

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;

// Ripped from nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request
public abstract class Request extends AbstractBTLEOperation<LefunDeviceSupport> {
    private Logger logger = (Logger) LoggerFactory.getLogger(getName());
    protected TransactionBuilder builder;

    protected Request(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support);
        this.builder = builder;
    }

    @Override
    protected void doPerform() throws IOException {
        BluetoothGattCharacteristic characteristic = getSupport()
                .getCharacteristic(LefunConstants.UUID_CHARACTERISTIC_LEFUN_WRITE);
        builder.write(characteristic, createRequest());
    }

    public abstract byte[] createRequest();

    public void handleResponse(byte[] data) {
    }

    public String getName() {
        Class thisClass = getClass();
        while (thisClass.isAnonymousClass()) thisClass = thisClass.getSuperclass();
        return thisClass.getSimpleName();
    }

    protected void log(String message) {
        logger.debug(message);
    }

    public abstract int getCommandId();

    public boolean expectsResponse() {
        return true;
    }

    protected void reportFailure(String message) {
        // TODO: Toast here or something
    }
}

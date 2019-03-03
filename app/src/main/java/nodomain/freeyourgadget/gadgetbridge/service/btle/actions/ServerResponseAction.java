/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Uwe Hermann

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEServerAction;

/**
 * Invokes a response on a given GATT characteristic read.
 * The result status will be made available asynchronously through the
 * {@link BluetoothGattCallback}
 */
public class ServerResponseAction extends BtLEServerAction {
    private static final Logger LOG = LoggerFactory.getLogger(ServerResponseAction.class);

    private final byte[] value;
    private final int requestId;
    private final int status;
    private final int offset;

    public ServerResponseAction(BluetoothDevice device, int requestId, int status, int offset, byte[] data) {
        super(device);
        this.value = data;
        this.requestId = requestId;
        this.status = status;
        this.offset = offset;
    }

    @Override
    public boolean run(BluetoothGattServer server) {
        return writeValue(server, getDevice(), requestId, status, offset, value);
    }

    protected boolean writeValue(BluetoothGattServer gattServer, BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("writing to server: " + device.getAddress() + ": " + Logging.formatBytes(value));
        }

        return gattServer.sendResponse(device, requestId, 0, offset, value);
    }

    protected final byte[] getValue() {
        return value;
    }

    @Override
    public boolean expectsResult() {
        return false;
    }
}

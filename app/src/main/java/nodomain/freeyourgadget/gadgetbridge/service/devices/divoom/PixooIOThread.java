/*  Copyright (C) 2023-2024 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.divoom;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.devices.nothing.NothingProtocol;

public class PixooIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(PixooIOThread.class);
    private final PixooProtocol mPixooProtocol;


    @Override
    protected void initialize() {
        write(mPixooProtocol.encodeReqestAlarms());

        setUpdateState(GBDevice.State.INITIALIZED);
    }

    public PixooIOThread(GBDevice device, Context context, PixooProtocol deviceProtocol,
                         PixooSupport PixooSupport, BluetoothAdapter bluetoothAdapter) {
        super(device, context, deviceProtocol, PixooSupport, bluetoothAdapter);
        mPixooProtocol = deviceProtocol;
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read " + bytes + " bytes. " + hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }
}

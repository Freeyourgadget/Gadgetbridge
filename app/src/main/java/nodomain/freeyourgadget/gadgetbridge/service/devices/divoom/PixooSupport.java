/*  Copyright (C) 2023 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.divoom;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class PixooSupport extends AbstractSerialDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(PixooSupport.class);


    @Override
    public void onSendConfiguration(String config) {
        super.onSendConfiguration(config);
    }

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    public synchronized PixooIOThread getDeviceIOThread() {
        return (PixooIOThread) super.getDeviceIOThread();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    protected GBDeviceProtocol createDeviceProtocol() {
        return new PixooProtocol(getDevice());
    }

    @Override
    public void onInstallApp(Uri uri) {
        getDeviceIOThread().write(((PixooProtocol) getDeviceProtocol()).encodeShowFrame(uri));
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new PixooIOThread(getDevice(), getContext(), (PixooProtocol) getDeviceProtocol(),
                PixooSupport.this, getBluetoothAdapter());
    }
}

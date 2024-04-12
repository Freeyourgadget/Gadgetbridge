/*  Copyright (C) 2021-2024 Arjan Schrijver, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.galaxy_buds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class GalaxyBudsDeviceSupport extends AbstractHeadphoneDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(GalaxyBudsDeviceSupport.class);

    @Override
    public void onSendConfiguration(String config) {
        super.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        super.onTestNewFunction();
    }

    @Override
    public synchronized GalaxyBudsIOThread getDeviceIOThread() {
        return (GalaxyBudsIOThread) super.getDeviceIOThread();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    protected GBDeviceProtocol createDeviceProtocol() {
        return new GalaxyBudsProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new GalaxyBudsIOThread(getDevice(), getContext(), (GalaxyBudsProtocol) getDeviceProtocol(),
                GalaxyBudsDeviceSupport.this, getBluetoothAdapter());
    }
}

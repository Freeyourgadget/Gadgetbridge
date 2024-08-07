/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.motion300;

import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class SoundcoreMotion300DeviceSupport extends AbstractHeadphoneDeviceSupport {
    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new SoundcoreMotion300Protocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new SoundcoreMotion300IOThread(
                getDevice(),
                getContext(),
                (SoundcoreMotion300Protocol)getDeviceProtocol(),
                SoundcoreMotion300DeviceSupport.this,
                getBluetoothAdapter());
    }
}

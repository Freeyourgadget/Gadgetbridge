/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.watchs3;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;

public class XiaomiWatchS3Coordinator extends XiaomiCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_xiaomi_watch_s3;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miwatch;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_miwatch_disabled;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.BT_CLASSIC;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // only saw it with eSIM in #3506, but allowing both just in case
        return Pattern.compile("^Xiaomi Watch S3( eSIM)? [0-9A-F]{4}$");
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }
}

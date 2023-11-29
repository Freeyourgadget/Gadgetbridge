/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.miband7;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuami2021FWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband7.MiBand7Support;

public class MiBand7Coordinator extends Huami2021Coordinator {
    @Override
    public boolean supports(final GBDeviceCandidate candidate) {
        final String name = candidate.getName();
        return name.startsWith(HuamiConst.XIAOMI_SMART_BAND7_NAME) && !name.contains("Pro");
    }

    @Override
    public AbstractHuami2021FWInstallHandler createFwInstallHandler(final Uri uri, final Context context) {
        return new MiBand7FWInstallHandler(uri, context);
    }

    @Override
    public boolean supportsAgpsUpdates() {
        return false;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return MiBand7Support.class;
    }

    @Override
    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return false;
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_miband7;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miband6;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_miband6_disabled;
    }
}

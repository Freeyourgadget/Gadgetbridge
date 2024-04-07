/* Copyright (C) 2024 Yoran Vulker

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
   along with this program. If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmiwatch4;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiInstallHandler;

public class RedmiWatch4Coordinator extends XiaomiCoordinator {

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmi_watch_4;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Redmi Watch 4 [0-9A-F]{4}$");
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public ConnectionType getConnectionType() {
        // this device likely supports both connection types if we indicate
        // that we are an iOS device in the final auth step
        return ConnectionType.BT_CLASSIC;
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        final XiaomiInstallHandler handler = new XiaomiInstallHandler(uri, context);

        return handler.isValid() ? handler : null;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_amazfit_bip;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_amazfit_bip_disabled;
    }

    @Override
    public int getWorldClocksSlotCount() {
        return 20;
    }
}

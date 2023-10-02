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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.miband8;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class MiBand8Coordinator extends XiaomiCoordinator {
    private final Pattern NAME_PATTTERN = Pattern.compile("^Xiaomi Smart Band 8 [A-Z0-9]{4}$");

    @NonNull
    @Override
    public DeviceType getSupportedType(final GBDeviceCandidate candidate) {
        if (NAME_PATTTERN.matcher(candidate.getName()).matches()) {
            return DeviceType.MIBAND8;
        }

        return DeviceType.UNKNOWN;
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        // TODO implement this
        return super.findInstallHandler(uri, context);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_miband8;
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

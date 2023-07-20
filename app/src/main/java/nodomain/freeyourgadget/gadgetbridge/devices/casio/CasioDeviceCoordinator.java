/*  Copyright (C) 2023 Johannes Krude

    based on code from BlueWatcher, https://github.com/masterjc/bluewatcher

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
package nodomain.freeyourgadget.gadgetbridge.devices.casio;

import java.util.Collection;
import java.util.Collections;

import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;

import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;

public abstract class CasioDeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid casioService = new ParcelUuid(CasioConstants.WATCH_FEATURES_SERVICE_UUID);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(casioService).build();
        return Collections.singletonList(filter);
    }

    @Override
    public String getManufacturer() {
        return "Casio";
    }
}

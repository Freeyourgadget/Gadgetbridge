/*  Copyright (C) 2022-2024 Damien Gaignon, Daniel Dakhno, José Rebelo,
    Noodlez

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
package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.asteroidos.AsteroidOSDeviceSupport;

public class AsteroidOSDeviceCoordinator extends AbstractDeviceCoordinator {

    @Override
    public String getManufacturer() {
        return "AsteroidOS";
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid asteroidUUID = ParcelUuid.fromString(AsteroidOSConstants.SERVICE_UUID.toString());
        ScanFilter serviceFilter = new ScanFilter.Builder().setServiceUuid(asteroidUUID).build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(serviceFilter);

        return filters;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        if (candidate.supportsService(AsteroidOSConstants.SERVICE_UUID))
            return true;
        // Some devices still can't see the service
        for (String codename : AsteroidOSConstants.KNOWN_DEVICE_CODENAMES)
            if (candidate.getName().equals(codename))
                return true;
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return AsteroidOSDeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_asteroidos;
    }
}

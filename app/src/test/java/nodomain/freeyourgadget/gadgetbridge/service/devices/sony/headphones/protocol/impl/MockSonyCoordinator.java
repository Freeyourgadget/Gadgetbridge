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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class MockSonyCoordinator extends SonyHeadphonesCoordinator {
    private final DeviceType deviceType = DeviceType.SONY_WH_1000XM3;

    private final List<SonyHeadphonesCapabilities> capabilities = new ArrayList<>();

    @NonNull
    @Override
    public boolean supports(final GBDeviceCandidate candidate) {
        return true;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sony_wh_1000xm5;
    }

    public void addCapability(final SonyHeadphonesCapabilities capability) {
        capabilities.add(capability);
    }

    public List<SonyHeadphonesCapabilities> getCapabilities() {
        return capabilities;
    }
}

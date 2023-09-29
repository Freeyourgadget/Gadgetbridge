/*  Copyright (C) 2017-2021 Daniele Gobbetti, João Paulo Barraca, José
    Rebelo, Quallenauge

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
package nodomain.freeyourgadget.gadgetbridge.devices.roidmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.roidmi.RoidmiSupport;

public class Roidmi1Coordinator extends RoidmiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(Roidmi1Coordinator.class);

    @NonNull
    @Override
    public boolean supports(final GBDeviceCandidate candidate) {
        try {
            final String name = candidate.getName();

            if (name != null && name.contains("睿米车载蓝牙播放器")) {
                return true;
            }
        } catch (final Exception ex) {
            LOG.error("unable to check device support", ex);
        }

        return false;
    }

    @Override
    public int getBatteryCount() {
        // Roidmi 1 does not have voltage support
        return 0;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return RoidmiSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_roidmi;
    }
}

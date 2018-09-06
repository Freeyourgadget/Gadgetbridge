/*  Copyright (C) 2018 José Rebelo

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

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class Roidmi3Coordinator extends RoidmiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(Roidmi3Coordinator.class);

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        try {
            BluetoothDevice device = candidate.getDevice();
            String name = device.getName();

            if (name != null && name.contains("Roidmi Music Blue C")) {
                LOG.warn("Found a Roidmi 3, but support is disabled.");
                return DeviceType.UNKNOWN; // TODO Roidmi 3 is not working atm
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.ROIDMI3;
    }

    @Override
    public boolean supportsRgbLedColor() {
        return true;
    }
}

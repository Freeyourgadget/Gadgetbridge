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
package nodomain.freeyourgadget.gadgetbridge.model;

import org.junit.Assert;
import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class DeviceTypeTest extends TestBase {
    @Test
    public void ensureNoMissingDeviceInfo() {
        // Check that all coordinators for all device types declare valid device names, icons and manufacturer
        for (final DeviceType deviceType : DeviceType.values()) {
            final DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();
            Assert.assertNotEquals("Device name for " + deviceType + " is 0", 0, coordinator.getDeviceNameResource());
            Assert.assertNotEquals("Device icon for " + deviceType + " is 0", 0, coordinator.getDefaultIconResource());
            Assert.assertNotEquals("Disabled device icon for " + deviceType + " is 0", 0, coordinator.getDisabledIconResource());
            Assert.assertNotEquals("Manufacturer for " + deviceType + " is null", null, coordinator.getManufacturer());
        }
    }
}

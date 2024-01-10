/*  Copyright (C) 2024 Damien Gaignon

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.honorband3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiLECoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class HonorBand3Coordinator extends HuaweiLECoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(HonorBand3Coordinator.class);

    @Override
    public String getManufacturer() {
        return "Honor";
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.HONORBAND3;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        try {
            String name = candidate.getName();
            if (name != null && name.toLowerCase().startsWith(HuaweiConstants.HO_BAND3_NAME)) {
                return true;
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return getHuaweiCoordinator().genericHuaweiSupportedDeviceSpecificSettings(new int[]{
                R.xml.devicesettings_heartrate_automatic_enable,
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_disable_find_phone_with_dnd,
        });
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_honor_band3;
    }
}

/*  Copyright (C) 2021-2023 Gaignon Damien
    Copyright (C) 2022-2023 MartinJM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweitalkbandb6;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiBRCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class HuaweiTalkBandB6Coordinator extends HuaweiBRCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiTalkBandB6Coordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.HUAWEITALKBANDB6;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        try {
            String name = candidate.getName();
            if (name != null && name.toLowerCase().startsWith(HuaweiConstants.HU_TALKBANDB6_NAME)) {
                return true;
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return false;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return getHuaweiCoordinator().genericHuaweiSupportedDeviceSpecificSettings(null);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_huawei_talk_band_b6;
    }
}

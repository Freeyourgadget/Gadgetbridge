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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiband6;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiLECoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;

public class HuaweiBand6Coordinator extends HuaweiLECoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiBand6Coordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.HUAWEIBAND6;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        try {
            String name = candidate.getName();
            if (name != null && name.toLowerCase().startsWith(HuaweiConstants.HU_BAND6_NAME)) {
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
    public boolean supportsSpo2() {
        return true;
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new HuaweiSpo2SampleProvider(device, session);
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return getHuaweiCoordinator().genericHuaweiSupportedDeviceSpecificSettings(new int[]{
                R.xml.devicesettings_heartrate_automatic_enable,
                R.xml.devicesettings_spo_automatic_enable,
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_disable_find_phone_with_dnd,
        });
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_huawei_band6;
    }
}

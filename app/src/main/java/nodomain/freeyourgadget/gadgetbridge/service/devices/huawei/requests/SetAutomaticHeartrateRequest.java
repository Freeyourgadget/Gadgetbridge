/*  Copyright (C) 2022-2023 MartinJM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetAutomaticHeartrateRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetAutomaticHeartrateRequest.class);

    public SetAutomaticHeartrateRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.EnableAutomaticHeartrate.id;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        boolean automaticHeartrateEnabled = GBApplication
                .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
                .getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_AUTOMATIC_ENABLE, false);
        if (automaticHeartrateEnabled)
            LOG.info("Attempting to enable automatic heartrate");
        else
            LOG.info("Attempting to disable automatic heartrate");
        try {
            return new FitnessData.EnableAutomaticHeartrate.Request(paramsProvider, automaticHeartrateEnabled).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}

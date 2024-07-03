/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GetBatteryLevelRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetBatteryLevelRequest.class);

    public GetBatteryLevelRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.BatteryLevel.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new DeviceConfig.BatteryLevel.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle Battery Level");

        if (!(receivedPacket instanceof DeviceConfig.BatteryLevel.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.BatteryLevel.Response.class);

        byte batteryLevel = ((DeviceConfig.BatteryLevel.Response) receivedPacket).level;
        getDevice().setBatteryLevel(batteryLevel);

        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
        batteryInfo.level = (int)batteryLevel & 0xff;
        this.supportProvider.evaluateGBDeviceEvent(batteryInfo);

        if (GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE, false)) {
            if (!this.supportProvider.startBatteryRunnerDelayed()) {
                GB.toast(getContext(), R.string.battery_polling_failed_start, Toast.LENGTH_SHORT, GB.ERROR);
                LOG.error("Failed to start the battery polling");
            }
        }
    }
}

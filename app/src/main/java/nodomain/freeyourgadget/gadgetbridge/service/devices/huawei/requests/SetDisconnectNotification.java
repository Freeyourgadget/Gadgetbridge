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

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DisconnectNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetDisconnectNotification extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetDisconnectNotification.class);

    public SetDisconnectNotification(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DisconnectNotification.id;
        this.commandId = DisconnectNotification.DisconnectNotificationSetting.id;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(supportProvider.getDeviceMac());
        boolean notificationEnable = sharedPrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_DISCONNECTNOTIF_NOSHED, true);
        if (notificationEnable) {
            LOG.info("Attempting to enable disconnect notification");
        } else {
            LOG.info("Attempting to disable disconnect notification");
        }
        try {
            return new DisconnectNotification.DisconnectNotificationSetting.Request(
                    paramsProvider,
                    notificationEnable
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}

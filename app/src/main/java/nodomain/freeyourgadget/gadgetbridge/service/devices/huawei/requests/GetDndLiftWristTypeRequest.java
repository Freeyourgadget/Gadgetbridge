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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetDndLiftWristTypeRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetDndLiftWristTypeRequest.class);

    public GetDndLiftWristTypeRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.DndLiftWristType.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsQueryDndLiftWristDisturbType();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new DeviceConfig.DndLiftWristType.Request(
                    paramsProvider
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle DND Allow Content");
        if (!(receivedPacket instanceof DeviceConfig.DndLiftWristType.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.DndLiftWristType.Response.class);
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(supportProvider.getDeviceMac());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(HuaweiConstants.PREF_HUAWEI_DND_LIFT_WRIST_TYPE,
                    ((DeviceConfig.DndLiftWristType.Response) receivedPacket).dndLiftWristType);
        editor.apply();
    }
}
/*  Copyright (C) 2024 Martin.JM

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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HeartRateZonesConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendHeartRateZonesConfig extends Request {

    public SendHeartRateZonesConfig(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = supportProvider.getHuaweiCoordinator().supportsExtendedHeartRateZones() ?
                FitnessData.HeartRateZoneConfigPacket.id_extended :
                FitnessData.HeartRateZoneConfigPacket.id_simple;
    }

    @Override
    protected boolean requestSupported() {
        return
                !supportProvider.getHuaweiCoordinator().supportsTrack() && // In this case it uses P2P
                supportProvider.getHuaweiCoordinator().supportsHeartRateZones();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            HeartRateZonesConfig heartRateZonesConfig = new HeartRateZonesConfig(HeartRateZonesConfig.TYPE_UPRIGHT, new ActivityUser().getAge());
            if (supportProvider.getHuaweiCoordinator().supportsExtendedHeartRateZones()) {
                return FitnessData.HeartRateZoneConfigPacket.Request.requestExtended(paramsProvider, heartRateZonesConfig).serialize();
            } else {
                return FitnessData.HeartRateZoneConfigPacket.Request.requestSimple(paramsProvider, heartRateZonesConfig).serialize();
            }
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}

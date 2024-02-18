/*  Copyright (C) 2024 Vitalii Tomin, Martin.JM

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

import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.GpsAndTime;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SendGpsAndTimeToDeviceRequest extends Request {

    public SendGpsAndTimeToDeviceRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = GpsAndTime.id;
        this.commandId = GpsAndTime.CurrentGPSRequest.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            Prefs prefs = GBApplication.getPrefs();
            return new GpsAndTime.CurrentGPSRequest(
                    this.paramsProvider,
                    (int) (Calendar.getInstance().getTime().getTime() / 1000L) - 60, // Backdating a bit seems to work better
                    prefs.getFloat("location_latitude", 0.0F),
                    prefs.getFloat("location_longitude", 0.0F)
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}

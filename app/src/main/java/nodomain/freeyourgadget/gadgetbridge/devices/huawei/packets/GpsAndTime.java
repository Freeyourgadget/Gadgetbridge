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

package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class GpsAndTime {
    public static final byte id = 0x18;

    public static class CurrentGPSRequest extends HuaweiPacket {
        public static final byte id = 0x07;
        public CurrentGPSRequest (
                ParamsProvider paramsProvider,
                int timestamp,
                double lat,
                double lon
        ) {
            super(paramsProvider);

            this.serviceId = GpsAndTime.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV()
                    .put(0x01, timestamp)
                    .put(0x02, lon)
                    .put(0x03, lat);
            this.isEncrypted = true;
            this.complete = true;
        }
    }
}

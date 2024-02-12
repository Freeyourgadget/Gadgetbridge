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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class FindPhone {
    public static final byte id = 0x0b;

    public static class Response extends HuaweiPacket {
        public static final byte id = 0x01;

        public boolean start = false;

        public Response(ParamsProvider paramsProvider) {
            super(paramsProvider);

            this.serviceId = FindPhone.id;
            this.commandId = id;

            this.isEncrypted = false;
        }

        @Override
        public void parseTlv() throws ParseException {
            if (this.tlv.contains(0x01))
                this.start = this.tlv.getBoolean(0x01);
            // No missing tag exception so it will stop by default
        }
    }

    public static class StopRequest extends HuaweiPacket {
        public static final byte id = 0x02;

        public StopRequest(ParamsProvider paramsProvider) {
            super(paramsProvider);

            this.serviceId = FindPhone.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV()
                    .put(0x01, (byte) 2);

            this.complete = true;
        }
    }
}
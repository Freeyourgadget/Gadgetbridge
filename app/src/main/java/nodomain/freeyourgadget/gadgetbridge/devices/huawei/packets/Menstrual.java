/*  Copyright (C) 2024 Damien Gaignon

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

public class Menstrual {
    public static final byte id = 0x32;

    public static class ModifyTime {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, int errorCode, long time) {
                super(paramsProvider);

                this.serviceId = Menstrual.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV();
                if (errorCode == 0) {
                    this.tlv.put(0x01, time);
                } else {
                    this.tlv.put(0x7f, (int)0x249F1);
                }
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Menstrual.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                // Do not know data yet
            }
        }
    }

    public static class CapabilityRequest extends HuaweiPacket {
        public static final byte id = 0x05;

        public CapabilityRequest(ParamsProvider paramsProvider) {
            super(paramsProvider);

            this.serviceId = Menstrual.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV()
                    .put(0x01, (byte)0x02);

            this.complete = true;
        }
    }
}

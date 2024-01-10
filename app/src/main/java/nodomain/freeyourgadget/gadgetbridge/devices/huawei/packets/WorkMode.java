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

public class WorkMode {
    public static final byte id = 0x26;

    /*
     * public static class ModeStatus {
     *     public static final byte id = 0x01;
     *     public static final int autoDetectMode  = 0x01;
     *     public static final int footWear  = 0x02;
     * }
     */

    public static class SwitchStatusRequest extends HuaweiPacket {
        public static final byte id = 0x02;
        public static final int setStatus  = 0x01;

        public SwitchStatusRequest(ParamsProvider paramsProvider, boolean autoWorkMode) {
            super(paramsProvider);

            this.serviceId = WorkMode.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV()
                    .put(0x01, autoWorkMode);

            this.complete = true;
        }
    }

    /*
     * public static class FootWear {
     *     public static final byte id = 0x03;
     *     public static final int AutoDetectMode  = 0x01;
     *     public static final int FootWear  = 0x02;
     * }
     */
}

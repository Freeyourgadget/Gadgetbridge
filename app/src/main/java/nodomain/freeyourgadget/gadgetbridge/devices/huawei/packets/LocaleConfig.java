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

public class LocaleConfig {
    public static final byte id = 0x0C;

    public static class SetLanguageSetting extends HuaweiPacket {
        public static final byte id = 0x01;

        public SetLanguageSetting(
                ParamsProvider paramsProvider,
                byte[] locale,
                byte measurement
        ) {
            super(paramsProvider);

            this.serviceId = LocaleConfig.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV()
                    .put(0x01, locale)
                    .put(0x02, measurement);

            this.complete = true;
        }
    }

    public static class MeasurementSystem {
        // TODO: enum?

        public static final byte metric = 0x00;
        public static final byte imperial = 0x01;
    }

    public static class SetTemperatureUnitSetting extends HuaweiPacket {
        public static final byte id = 0x05;

        public SetTemperatureUnitSetting(
                ParamsProvider paramsProvider,
                byte isFahrenheit
        ) {
            super(paramsProvider);

            this.serviceId = LocaleConfig.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV()
                    .put(0x01, isFahrenheit);

            this.complete = true;
        }
    }

}

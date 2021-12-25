/*  Copyright (C) 2021 José Rebelo

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params;

public enum BatteryType {
    SINGLE(0x00),
    DUAL(0x01),
    CASE(0x02);

    private final byte code;

    BatteryType(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return this.code;
    }

    public static BatteryType fromCode(final byte code) {
        for (final BatteryType batteryType : values()) {
            if (batteryType.code == code) {
                return batteryType;
            }
        }

        return null;
    }
}

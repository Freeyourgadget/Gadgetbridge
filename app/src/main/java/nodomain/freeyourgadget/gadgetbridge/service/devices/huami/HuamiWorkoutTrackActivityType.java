/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import java.util.Locale;

/**
 * The workout types, used to start / when workout tracking starts on the band.
 */
public enum HuamiWorkoutTrackActivityType {
    OutdoorRunning(0x01),
    Walking(0x04),
    Treadmill(0x02),
    OutdoorCycling(0x03),
    IndoorCycling(0x09),
    Elliptical(0x06),
    PoolSwimming(0x05),
    Freestyle(0x0b),
    JumpRope(0x08),
    RowingMachine(0x07),
    Yoga(0x0a);

    private final byte code;

    HuamiWorkoutTrackActivityType(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }

    public static HuamiWorkoutTrackActivityType fromCode(final byte code) {
        for (final HuamiWorkoutTrackActivityType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }

        return null;
    }
}

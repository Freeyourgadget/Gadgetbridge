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

/**
 * The workout types, used to start / when workout tracking starts on the band.
 */
public enum Huami2021WorkoutTrackActivityType {
    // TODO 150 workouts :/
    Badminton(0x5c),
    Dance(0x4c),
    Elliptical(0x09),
    Freestyle(0x05),
    IndoorCycling(0x08),
    IndoorFitness(0x18),
    JumpRope(0x15),
    OutdoorCycling(0x04),
    OutdoorRunning(0x01),
    PoolSwimming(0x06),
    Rowing(0x17),
    Soccer(0xbf),
    Treadmill(0x02),
    Walking(0x03),
    Yoga(0x3c),
    ;

    private final byte code;

    Huami2021WorkoutTrackActivityType(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }

    public static Huami2021WorkoutTrackActivityType fromCode(final byte code) {
        for (final Huami2021WorkoutTrackActivityType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }

        return null;
    }
}

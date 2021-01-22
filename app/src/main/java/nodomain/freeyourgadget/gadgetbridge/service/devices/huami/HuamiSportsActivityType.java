/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer

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

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public enum HuamiSportsActivityType {
    OutdoorRunning(1),
    Treadmill(2),
    Walking(3),
    Cycling(4),
    Exercise(5),
    Swimming(6),
    OpenWaterSwimming(7),
    IndoorCycling(8),
    EllipticalTrainer(9),
    Soccer(0x12),
    JumpRope(0x15),
    RowingMachine(0x17),
    StrengthTraining(0x34),
    Yoga(0x3c),
    Cricket(0x4e),
    Basketball(0x55),
    PingPong(0x59),
    Badminton(0x5c);


    private final int code;

    HuamiSportsActivityType(final int code) {
        this.code = code;
    }

    public int toActivityKind() {
        switch (this) {
            case OutdoorRunning:
                return ActivityKind.TYPE_RUNNING;
            case Treadmill:
                return ActivityKind.TYPE_TREADMILL;
            case Cycling:
                return ActivityKind.TYPE_CYCLING;
            case Walking:
                return ActivityKind.TYPE_WALKING;
            case Exercise:
                return ActivityKind.TYPE_EXERCISE;
            case Swimming:
                return ActivityKind.TYPE_SWIMMING;
            case OpenWaterSwimming:
                return ActivityKind.TYPE_SWIMMING_OPENWATER;
            case IndoorCycling:
                return ActivityKind.TYPE_INDOOR_CYCLING;
            case EllipticalTrainer:
                return ActivityKind.TYPE_ELLIPTICAL_TRAINER;
            case Soccer:
                return ActivityKind.TYPE_SOCCER;
            case JumpRope:
                return ActivityKind.TYPE_JUMP_ROPING;
            case RowingMachine:
                return ActivityKind.TYPE_ROWING_MACHINE;
            case Yoga:
                return ActivityKind.TYPE_YOGA;
            case Cricket:
                return ActivityKind.TYPE_CRICKET;
            case Basketball:
                return ActivityKind.TYPE_BASKETBALL;
            case PingPong:
                return ActivityKind.TYPE_PINGPONG;
            case Badminton:
                return ActivityKind.TYPE_BADMINTON;
            case StrengthTraining:
                return ActivityKind.TYPE_STRENGTH_TRAINING;
        }
        throw new RuntimeException("Not mapped activity kind for: " + this);
    }

    public static HuamiSportsActivityType fromCode(int huamiCode) {
        for (HuamiSportsActivityType type : values()) {
            if (type.code == huamiCode) {
                return type;
            }
        }
        throw new RuntimeException("No matching HuamiSportsActivityType for code: " + huamiCode);
    }

    public static HuamiSportsActivityType fromActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_RUNNING:
                return OutdoorRunning;
            case ActivityKind.TYPE_TREADMILL:
                return Treadmill;
            case ActivityKind.TYPE_CYCLING:
                return Cycling;
            case ActivityKind.TYPE_WALKING:
                return Walking;
            case ActivityKind.TYPE_EXERCISE:
                return Exercise;
            case ActivityKind.TYPE_SWIMMING:
                return Swimming;
            case ActivityKind.TYPE_SWIMMING_OPENWATER:
                return OpenWaterSwimming;
            case ActivityKind.TYPE_INDOOR_CYCLING:
                return IndoorCycling;
            case ActivityKind.TYPE_ELLIPTICAL_TRAINER:
                return EllipticalTrainer;
            case ActivityKind.TYPE_SOCCER:
                return Soccer;
            case ActivityKind.TYPE_JUMP_ROPING:
                return JumpRope;
            case ActivityKind.TYPE_ROWING_MACHINE:
                return RowingMachine;
            case ActivityKind.TYPE_YOGA:
                return Yoga;
            case ActivityKind.TYPE_CRICKET:
                return Cricket;
            case ActivityKind.TYPE_BASKETBALL:
                return Basketball;
            case ActivityKind.TYPE_PINGPONG:
                return PingPong;
            case ActivityKind.TYPE_BADMINTON:
                return Badminton;
            case ActivityKind.TYPE_STRENGTH_TRAINING:
                return StrengthTraining;

        }
        throw new RuntimeException("No matching activity activityKind: " + activityKind);
    }
}

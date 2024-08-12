/*  Copyright (C) 2019-2024 Andreas Shimokawa, Sebastian Krey, Your Name

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
    OutdoorHiking(15),
    Climbing(10),
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

    public ActivityKind toActivityKind() {
        switch (this) {
            case OutdoorRunning:
                return ActivityKind.RUNNING;
            case OutdoorHiking:
                return ActivityKind.HIKING;
            case Climbing:
                return ActivityKind.CLIMBING;
            case Treadmill:
                return ActivityKind.TREADMILL;
            case Cycling:
                return ActivityKind.CYCLING;
            case Walking:
                return ActivityKind.WALKING;
            case Exercise:
                return ActivityKind.EXERCISE;
            case Swimming:
                return ActivityKind.SWIMMING;
            case OpenWaterSwimming:
                return ActivityKind.SWIMMING_OPENWATER;
            case IndoorCycling:
                return ActivityKind.INDOOR_CYCLING;
            case EllipticalTrainer:
                return ActivityKind.ELLIPTICAL_TRAINER;
            case Soccer:
                return ActivityKind.SOCCER;
            case JumpRope:
                return ActivityKind.JUMP_ROPING;
            case RowingMachine:
                return ActivityKind.ROWING_MACHINE;
            case Yoga:
                return ActivityKind.YOGA;
            case Cricket:
                return ActivityKind.CRICKET;
            case Basketball:
                return ActivityKind.BASKETBALL;
            case PingPong:
                return ActivityKind.PINGPONG;
            case Badminton:
                return ActivityKind.BADMINTON;
            case StrengthTraining:
                return ActivityKind.STRENGTH_TRAINING;
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

    public static HuamiSportsActivityType fromActivityKind(ActivityKind activityKind) {
        switch (activityKind) {
            case RUNNING:
                return OutdoorRunning;
            case HIKING:
                return OutdoorHiking;
            case CLIMBING:
                return Climbing;
            case TREADMILL:
                return Treadmill;
            case CYCLING:
                return Cycling;
            case WALKING:
                return Walking;
            case EXERCISE:
                return Exercise;
            case SWIMMING:
                return Swimming;
            case SWIMMING_OPENWATER:
                return OpenWaterSwimming;
            case INDOOR_CYCLING:
                return IndoorCycling;
            case ELLIPTICAL_TRAINER:
                return EllipticalTrainer;
            case SOCCER:
                return Soccer;
            case JUMP_ROPING:
                return JumpRope;
            case ROWING_MACHINE:
                return RowingMachine;
            case YOGA:
                return Yoga;
            case CRICKET:
                return Cricket;
            case BASKETBALL:
                return Basketball;
            case PINGPONG:
                return PingPong;
            case BADMINTON:
                return Badminton;
            case STRENGTH_TRAINING:
                return StrengthTraining;

        }
        throw new RuntimeException("No matching activity activityKind: " + activityKind);
    }
}

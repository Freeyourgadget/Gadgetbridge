/*  Copyright (C) 2021 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.activity;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiWorkoutScreenActivityType;

public enum WithingsActivityType {

    WALKING(1),
    RUNNING(2),
    HIKING(3),
    BIKING(6),
    SWIMMING(7),
    SURFING(8),
    KITESURFING(9),
    WINDSURFING(10),
    TENNIS(12),
    PINGPONG(13),
    SQUASH(14),
    BADMINTON(15),
    WEIGHTLIFTING(16),
    GYMNASTICS(17),
    ELLIPTICAL(18),
    PILATES(19),
    BASKETBALL(20),
    SOCCER(21),
    FOOTBALL(22),
    RUGBY(23),
    VOLLEYBALL(24),
    GOLFING(227),
    YOGA(28),
    DANCING(29),
    BOXING(30),
    SKIING(34),
    SNOWBOARDING(35),
    ROWING(0), // The code has yet to be identified.
    ZUMBA(188),
    BASEBALL(191),
    HANDBALL(192),
    HOCKEY(193),
    ICEHOCKEY(194),
    CLIMBING(195),
    ICESKATING(196),
    RIDING(26),
    OTHER(36);

    private int code;

    WithingsActivityType(int typeCode) {
        this.code = typeCode;
    }

    public static WithingsActivityType fromCode(int withingsCode) {
        for (WithingsActivityType type : values()) {
            if (type.code == withingsCode) {
                return type;
            }
        }
        throw new RuntimeException("No matching WithingsActivityType for code: " + withingsCode);
    }

    public int getCode() {
        return code;
    }

    public int toActivityKind() {
        switch (this) {
            case WALKING:
                return ActivityKind.TYPE_WALKING;
            case RUNNING:
                return ActivityKind.TYPE_RUNNING;
            case HIKING:
                return ActivityKind.TYPE_HIKING;
            case BIKING:
                return ActivityKind.TYPE_CYCLING;
            case SWIMMING:
                return ActivityKind.TYPE_SWIMMING;
            case SURFING:
                return ActivityKind.TYPE_ACTIVITY;
            case KITESURFING:
                return ActivityKind.TYPE_ACTIVITY;
            case WINDSURFING:
                return ActivityKind.TYPE_ACTIVITY;
            case TENNIS:
                return ActivityKind.TYPE_ACTIVITY;
            case PINGPONG:
                return ActivityKind.TYPE_PINGPONG;
            case SQUASH:
                return ActivityKind.TYPE_ACTIVITY;
            case BADMINTON:
                return ActivityKind.TYPE_BADMINTON;
            case WEIGHTLIFTING:
                return ActivityKind.TYPE_ACTIVITY;
            case GYMNASTICS:
                return ActivityKind.TYPE_EXERCISE;
            case ELLIPTICAL:
                return ActivityKind.TYPE_ELLIPTICAL_TRAINER;
            case PILATES:
                return ActivityKind.TYPE_YOGA;
            case BASKETBALL:
                return ActivityKind.TYPE_BASKETBALL;
            case SOCCER:
                return ActivityKind.TYPE_SOCCER;
            case FOOTBALL:
                return ActivityKind.TYPE_ACTIVITY;
            case RUGBY:
                return ActivityKind.TYPE_ACTIVITY;
            case VOLLEYBALL:
                return ActivityKind.TYPE_ACTIVITY;
            case GOLFING:
                return ActivityKind.TYPE_ACTIVITY;
            case YOGA:
                return ActivityKind.TYPE_YOGA;
            case DANCING:
                return ActivityKind.TYPE_ACTIVITY;
            case BOXING:
                return ActivityKind.TYPE_ACTIVITY;
            case SKIING:
                return ActivityKind.TYPE_ACTIVITY;
            case SNOWBOARDING:
                return ActivityKind.TYPE_ACTIVITY;
            case ROWING:
                return ActivityKind.TYPE_ROWING_MACHINE;
            case ZUMBA:
                return ActivityKind.TYPE_ACTIVITY;
            case BASEBALL:
                return ActivityKind.TYPE_CRICKET;
            case HANDBALL:
                return ActivityKind.TYPE_ACTIVITY;
            case HOCKEY:
                return ActivityKind.TYPE_ACTIVITY;
            case ICEHOCKEY:
                return ActivityKind.TYPE_ACTIVITY;
            case CLIMBING:
                return ActivityKind.TYPE_CLIMBING;
            case ICESKATING:
                return ActivityKind.TYPE_ACTIVITY;
            default:
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    public static WithingsActivityType fromPrefValue(final String prefValue) {
        for (final WithingsActivityType type : values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(prefValue.replace("_", "").toLowerCase(Locale.ROOT))) {
                return type;
            }
        }
        throw new RuntimeException("No matching WithingsActivityType for pref value: " + prefValue);
    }
}

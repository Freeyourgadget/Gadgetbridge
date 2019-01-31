/*  Copyright (C) 2017-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public enum BipActivityType {
    Outdoor(1),
    Treadmill(2),
    Walking(3),
    Cycling(4),
    Exercise(5);

    private final int code;

    BipActivityType(final int code) {
        this.code = code;
    }

    public int toActivityKind() {
        switch (this) {
            case Outdoor:
                return ActivityKind.TYPE_RUNNING;
            case Treadmill:
                return ActivityKind.TYPE_TREADMILL;
            case Cycling:
                return ActivityKind.TYPE_CYCLING;
            case Walking:
                return ActivityKind.TYPE_WALKING;
            case Exercise:
                return ActivityKind.TYPE_EXERCISE;
        }
        throw new RuntimeException("Not mapped activity kind for: " + this);
    }

    public static BipActivityType fromCode(int bipCode) {
        for (BipActivityType type : values()) {
            if (type.code == bipCode) {
                return type;
            }
        }
        throw new RuntimeException("No matching BipActivityType for code: " + bipCode);
    }

    public static BipActivityType fromActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_RUNNING:
                return Outdoor;
            case ActivityKind.TYPE_TREADMILL:
                return Treadmill;
            case ActivityKind.TYPE_CYCLING:
                return Cycling;
            case ActivityKind.TYPE_WALKING:
                return Walking;
            case ActivityKind.TYPE_EXERCISE:
                return Exercise;
        }
        throw new RuntimeException("No matching activity activityKind: " + activityKind);
    }
}

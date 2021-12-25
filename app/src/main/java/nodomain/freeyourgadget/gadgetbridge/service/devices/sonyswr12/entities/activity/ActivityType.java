/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public enum ActivityType {
    WALK(1),
    RUN(2),
    SLEEP(3),
    HEART_RATE(10),
    END(14);

    final int value;

    ActivityType(int value) {
        this.value = value;
    }

    public static ActivityType fromInt(int i) {
        for (ActivityType type : values()){
            if (type.value == i)
                return type;
        }
        throw new IllegalArgumentException("wrong activity type: " + i);
    }
}

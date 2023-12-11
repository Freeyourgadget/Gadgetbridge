/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;

public class XiaomiWorkoutType {
    private final int code;
    private final String name;

    public XiaomiWorkoutType(final int code, final String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @StringRes
    public static int mapWorkoutName(final int code) {
        switch (code) {
            case 1:
                return R.string.activity_type_outdoor_running;
            case 2:
                return R.string.activity_type_walking;
            case 3:
                return R.string.activity_type_hiking;
            case 4:
                return R.string.activity_type_trekking;
            case 5:
                return R.string.activity_type_trail_run;
            case 6:
                return R.string.activity_type_outdoor_cycling;
            case 501:
                return R.string.activity_type_wrestling;
        }

        return -1;
    }
}

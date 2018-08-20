/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class SleepUtils {
    public static final float Y_VALUE_DEEP_SLEEP = 0.2f;
    public static final float Y_VALUE_LIGHT_SLEEP = 0.3f;

    public static boolean isSleep(byte type) {
        return type == ActivityKind.TYPE_DEEP_SLEEP || type == ActivityKind.TYPE_LIGHT_SLEEP;
    }
}

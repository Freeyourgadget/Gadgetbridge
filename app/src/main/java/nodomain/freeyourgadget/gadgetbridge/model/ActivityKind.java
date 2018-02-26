/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public class ActivityKind {
    public static final int TYPE_NOT_MEASURED = -1;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_ACTIVITY = 1;
    public static final int TYPE_LIGHT_SLEEP = 2;
    public static final int TYPE_DEEP_SLEEP = 4;
    public static final int TYPE_NOT_WORN = 8;

    public static final int TYPE_SLEEP = TYPE_LIGHT_SLEEP | TYPE_DEEP_SLEEP;
    public static final int TYPE_ALL = TYPE_ACTIVITY | TYPE_SLEEP | TYPE_NOT_WORN;

    public static int[] mapToDBActivityTypes(int types, SampleProvider provider) {
        int[] result = new int[3];
        int i = 0;
        if ((types & ActivityKind.TYPE_ACTIVITY) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_ACTIVITY);
        }
        if ((types & ActivityKind.TYPE_DEEP_SLEEP) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_DEEP_SLEEP);
        }
        if ((types & ActivityKind.TYPE_LIGHT_SLEEP) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_LIGHT_SLEEP);
        }
        if ((types & ActivityKind.TYPE_NOT_WORN) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_NOT_WORN);
        }
        return Arrays.copyOf(result, i);
    }

}

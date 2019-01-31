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

import android.content.Context;

import java.util.Arrays;

import androidx.annotation.DrawableRes;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public class ActivityKind {
    public static final int TYPE_NOT_MEASURED = -1;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_ACTIVITY = 1;
    public static final int TYPE_LIGHT_SLEEP = 2;
    public static final int TYPE_DEEP_SLEEP = 4;
    public static final int TYPE_NOT_WORN = 8;
    public static final int TYPE_RUNNING = 16;
    public static final int TYPE_WALKING = 32;
    public static final int TYPE_SWIMMING = 64;
    public static final int TYPE_CYCLING = 128;
    public static final int TYPE_TREADMILL = 256;
    public static final int TYPE_EXERCISE = 512;

    private static final int TYPES_COUNT = 12;

    public static final int TYPE_SLEEP = TYPE_LIGHT_SLEEP | TYPE_DEEP_SLEEP;
    public static final int TYPE_ALL = TYPE_ACTIVITY | TYPE_SLEEP | TYPE_NOT_WORN;

    public static int[] mapToDBActivityTypes(int types, SampleProvider provider) {
        int[] result = new int[TYPES_COUNT];
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
        if ((types & ActivityKind.TYPE_RUNNING) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_RUNNING);
        }
        if ((types & ActivityKind.TYPE_WALKING) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_WALKING);
        }
        if ((types & ActivityKind.TYPE_SWIMMING) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_SWIMMING);
        }
        if ((types & ActivityKind.TYPE_CYCLING) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_CYCLING);
        }
        if ((types & ActivityKind.TYPE_TREADMILL) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_TREADMILL);
        }
        if ((types & ActivityKind.TYPE_EXERCISE) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_EXERCISE);
        }
        return Arrays.copyOf(result, i);
    }

    public static String asString(int kind, Context context) {
        switch (kind) {
            case TYPE_NOT_MEASURED:
                return context.getString(R.string.activity_type_not_measured);
            case TYPE_ACTIVITY:
                return context.getString(R.string.activity_type_activity);
            case TYPE_LIGHT_SLEEP:
                return context.getString(R.string.activity_type_light_sleep);
            case TYPE_DEEP_SLEEP:
                return context.getString(R.string.activity_type_deep_sleep);
            case TYPE_NOT_WORN:
                return context.getString(R.string.activity_type_not_worn);
            case TYPE_RUNNING:
                return context.getString(R.string.activity_type_running);
            case TYPE_WALKING:
                return context.getString(R.string.activity_type_walking);
            case TYPE_SWIMMING:
                return context.getString(R.string.activity_type_swimming);
            case TYPE_CYCLING:
                return context.getString(R.string.activity_type_biking);
            case TYPE_TREADMILL:
                return context.getString(R.string.activity_type_treadmill);
            case TYPE_EXERCISE:
                return context.getString(R.string.activity_type_exercise);
            case TYPE_UNKNOWN:
            default:
                return context.getString(R.string.activity_type_unknown);
        }
    }

    @DrawableRes
    public static int getIconId(int kind) {
        switch (kind) {
            case TYPE_NOT_MEASURED:
                return R.drawable.ic_activity_not_measured;
            case TYPE_LIGHT_SLEEP:
                return R.drawable.ic_activity_light_sleep;
            case TYPE_DEEP_SLEEP:
                return R.drawable.ic_activity_deep_sleep;
            case TYPE_RUNNING:
                return R.drawable.ic_activity_running;
            case TYPE_WALKING:
                return R.drawable.ic_activity_walking;
            case TYPE_CYCLING:
                return R.drawable.ic_activity_biking;
            case TYPE_TREADMILL:
                return R.drawable.ic_activity_walking;
            case TYPE_EXERCISE: // fall through
            case TYPE_SWIMMING: // fall through
            case TYPE_NOT_WORN: // fall through
            case TYPE_ACTIVITY: // fall through
            case TYPE_UNKNOWN: // fall through
            default:
                return R.drawable.ic_activity_unknown;
        }
    }
}

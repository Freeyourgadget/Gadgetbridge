/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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

import androidx.annotation.DrawableRes;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public class ActivityKind {
    public static final int TYPE_NOT_MEASURED = -1;
    public static final int TYPE_UNKNOWN = 0x00000000;
    public static final int TYPE_ACTIVITY = 0x00000001;
    public static final int TYPE_LIGHT_SLEEP = 0x00000002;
    public static final int TYPE_DEEP_SLEEP = 0x00000004;
    public static final int TYPE_NOT_WORN = 0x00000008;
    public static final int TYPE_RUNNING = 0x00000010;
    public static final int TYPE_WALKING = 0x00000020;
    public static final int TYPE_SWIMMING = 0x00000040;
    public static final int TYPE_CYCLING = 0x00000080;
    public static final int TYPE_TREADMILL = 0x00000100;
    public static final int TYPE_EXERCISE = 0x00000200;
    public static final int TYPE_SWIMMING_OPENWATER = 0x00000400;
    public static final int TYPE_INDOOR_CYCLING = 0x00000800;
    public static final int TYPE_ELLIPTICAL_TRAINER = 0x00001000;
    public static final int TYPE_JUMP_ROPING = 0x00002000;
    public static final int TYPE_YOGA = 0x00004000;

    private static final int TYPES_COUNT = 17;

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
        if ((types & ActivityKind.TYPE_SWIMMING_OPENWATER) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_SWIMMING_OPENWATER);
        }
        if ((types & ActivityKind.TYPE_INDOOR_CYCLING) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_INDOOR_CYCLING);
        }
        if ((types & ActivityKind.TYPE_ELLIPTICAL_TRAINER) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_ELLIPTICAL_TRAINER);
        }
        if ((types & ActivityKind.TYPE_JUMP_ROPING) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_JUMP_ROPING);
        }
        if ((types & ActivityKind.TYPE_YOGA) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_YOGA);
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
            case TYPE_SWIMMING_OPENWATER:
                return context.getString(R.string.activity_type_swimming_openwater);
            case TYPE_INDOOR_CYCLING:
                return context.getString(R.string.activity_type_indoor_cycling);
            case TYPE_ELLIPTICAL_TRAINER:
                return context.getString(R.string.activity_type_elliptical_trainer);
            case TYPE_JUMP_ROPING:
                return context.getString(R.string.activity_type_jump_roping);
            case TYPE_YOGA:
                return context.getString(R.string.activity_type_yoga);
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
                return R.drawable.ic_activity_sleep;
            case TYPE_DEEP_SLEEP:
                return R.drawable.ic_activity_sleep;
            case TYPE_RUNNING:
                return R.drawable.ic_activity_running;
            case TYPE_WALKING:
                return R.drawable.ic_activity_walking;
            case TYPE_CYCLING:
                return R.drawable.ic_activity_biking;
            case TYPE_TREADMILL:
                return R.drawable.ic_activity_threadmill;
            case TYPE_EXERCISE:
                return R.drawable.ic_activity_exercise;
            case TYPE_SWIMMING:
            case TYPE_SWIMMING_OPENWATER:
                return R.drawable.ic_activity_swimming;
            case TYPE_INDOOR_CYCLING:
                return R.drawable.ic_activity_bike_trainer;
            case TYPE_ELLIPTICAL_TRAINER:
                return R.drawable.ic_activity_eliptical;
            case TYPE_JUMP_ROPING:
                return R.drawable.ic_activity_rope_jump;
            case TYPE_YOGA:
                return R.drawable.ic_activity_yoga;
            case TYPE_NOT_WORN: // fall through
            case TYPE_ACTIVITY: // fall through
            case TYPE_UNKNOWN: // fall through
            default:
                return R.drawable.ic_activity_unknown;
        }
    }
}

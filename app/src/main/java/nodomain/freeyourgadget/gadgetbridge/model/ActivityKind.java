/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Petr Vaněk, Sebastian Krey, Your Name

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
package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;

public enum ActivityKind {
    NOT_MEASURED(-1, R.string.activity_type_not_measured, R.drawable.ic_activity_not_measured),
    UNKNOWN(0x00000000, R.string.activity_type_unknown),
    ACTIVITY(0x00000001, R.string.activity_type_activity),
    LIGHT_SLEEP(0x00000002, R.string.activity_type_light_sleep, R.drawable.ic_activity_sleep),
    DEEP_SLEEP(0x00000004, R.string.activity_type_deep_sleep, R.drawable.ic_activity_sleep),
    NOT_WORN(0x00000008, R.string.activity_type_not_worn),
    RUNNING(0x00000010, R.string.activity_type_running, R.drawable.ic_activity_running),
    WALKING(0x00000020, R.string.activity_type_walking, R.drawable.ic_activity_walking),
    SWIMMING(0x00000040, R.string.activity_type_swimming, R.drawable.ic_activity_swimming),
    CYCLING(0x00000080, R.string.activity_type_biking, R.drawable.ic_activity_biking),
    TREADMILL(0x00000100, R.string.activity_type_treadmill, R.drawable.ic_activity_threadmill),
    EXERCISE(0x00000200, R.string.activity_type_exercise, R.drawable.ic_activity_exercise),
    SWIMMING_OPENWATER(0x00000400, R.string.activity_type_swimming_openwater, R.drawable.ic_activity_swimming),
    INDOOR_CYCLING(0x00000800, R.string.activity_type_indoor_cycling, R.drawable.ic_activity_bike_trainer),
    ELLIPTICAL_TRAINER(0x00001000, R.string.activity_type_elliptical_trainer, R.drawable.ic_activity_eliptical),
    JUMP_ROPING(0x00002000, R.string.activity_type_jump_roping, R.drawable.ic_activity_rope_jump),
    YOGA(0x00004000, R.string.activity_type_yoga, R.drawable.ic_activity_yoga),
    SOCCER(0x00008000, R.string.activity_type_soccer, R.drawable.ic_activity_soccer),
    ROWING_MACHINE(0x00010000, R.string.activity_type_rowing_machine, R.drawable.ic_activity_rowing),
    CRICKET(0x00020000, R.string.activity_type_cricket, R.drawable.ic_activity_cricket),
    BASKETBALL(0x00040000, R.string.activity_type_basketball, R.drawable.ic_activity_basketball),
    PINGPONG(0x00080000, R.string.activity_type_pingpong, R.drawable.ic_activity_pingpong),
    BADMINTON(0x00100000, R.string.activity_type_badminton, R.drawable.ic_activity_badmington),
    STRENGTH_TRAINING(0x00200000, R.string.activity_type_strength_training),
    HIKING(0x00400000, R.string.activity_type_hiking, R.drawable.ic_activity_hiking),
    CLIMBING(0x00800000, R.string.activity_type_climbing, R.drawable.ic_activity_climbing),
    REM_SLEEP(0x01000000, R.string.abstract_chart_fragment_kind_rem_sleep, R.drawable.ic_activity_sleep),
    SLEEP_ANY(0x00000002 | 0x00000004 | 0x01000000 | 0x02000000, R.string.menuitem_sleep, R.drawable.ic_activity_sleep),
    AWAKE_SLEEP(0x02000000, R.string.abstract_chart_fragment_kind_awake_sleep, R.drawable.ic_activity_sleep),

    // FIXME: Deprecate these - they're just kept around while we do not support reading from the old db
    VIVOMOVE_HR_TRANSITION(0x00000001 | 0x00000010 | 0x00000020 | 0x00000200 | 0x00000040, R.string.transition),
    ;

    private final int code;
    private final int label;
    private final int icon;

    ActivityKind(final int code) {
        this(code, R.string.activity_type_unknown);
    }

    ActivityKind(final int code, @StringRes final int label) {
        this(code, label, R.drawable.ic_activity_unknown_small);
    }

    ActivityKind(final int code, @StringRes final int label, @DrawableRes final int icon) {
        this.code = code;
        this.label = label;
        this.icon = icon;
    }

    public int getCode() {
        return code;
    }

    @StringRes
    public int getLabel() {
        return label;
    }

    public String getLabel(final Context context) {
        return context.getString(label);
    }

    @DrawableRes
    public int getIcon() {
        return icon;
    }

    public static ActivityKind fromCode(final int code) {
        for (final ActivityKind kind : ActivityKind.values()) {
            if (kind.code == code) {
                return kind;
            }
        }

        //throw new IllegalArgumentException("Unknown ActivityKind code " + code);
        return UNKNOWN;
    }

    public static boolean isSleep(final ActivityKind activityKind) {
        return activityKind == ActivityKind.SLEEP_ANY
                || activityKind == ActivityKind.LIGHT_SLEEP
                || activityKind == ActivityKind.DEEP_SLEEP
                || activityKind == ActivityKind.REM_SLEEP
                || activityKind == ActivityKind.AWAKE_SLEEP;
    }
}

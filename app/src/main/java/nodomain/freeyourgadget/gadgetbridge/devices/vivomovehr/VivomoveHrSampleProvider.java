/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.VivomoveHrActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.VivomoveHrActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class VivomoveHrSampleProvider extends AbstractSampleProvider<VivomoveHrActivitySample> {
    public static final int RAW_TYPE_KIND_MASK = 0x0F000000;
    public static final int RAW_TYPE_KIND_ACTIVITY = 0x00000000;
    public static final int RAW_TYPE_KIND_SLEEP = 0x01000000;
    // public static final int RAW_TYPE_KIND_NOT_WORN = 0x0F000000;
    public static final int RAW_NOT_WORN = 0x0F000000;

    public VivomoveHrSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public int normalizeType(int rawType) {
        if (rawType == RAW_NOT_WORN) {
            return ActivityKind.TYPE_NOT_WORN;
        }

        switch (rawType & RAW_TYPE_KIND_MASK) {
            case RAW_TYPE_KIND_ACTIVITY:
                return normalizeActivityType(rawType & ~RAW_TYPE_KIND_MASK);
            case RAW_TYPE_KIND_SLEEP:
                return normalizeSleepType(rawType & ~RAW_TYPE_KIND_MASK);
            default:
                // ???
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    private static int normalizeActivityType(int rawType) {
        switch (rawType) {
            case 0:
                // generic
                return ActivityKind.TYPE_ACTIVITY;
            case 1:
                // running
                return ActivityKind.TYPE_RUNNING;
            case 2:
                // cycling
                return ActivityKind.TYPE_CYCLING;
            case 3:
                // transition
                return ActivityKind.TYPE_ACTIVITY | ActivityKind.TYPE_RUNNING | ActivityKind.TYPE_WALKING | ActivityKind.TYPE_EXERCISE | ActivityKind.TYPE_SWIMMING;
            case 4:
                // fitness_equipment
                return ActivityKind.TYPE_EXERCISE;
            case 5:
                // swimming
                return ActivityKind.TYPE_SWIMMING;
            case 6:
                // walking
                return ActivityKind.TYPE_WALKING;
            case 8:
                // sedentary
                // TODO?
                return ActivityKind.TYPE_ACTIVITY;

            default:
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    private static int normalizeSleepType(int rawType) {
        switch (rawType) {
            case 0:
                // deep_sleep
                return ActivityKind.TYPE_DEEP_SLEEP;
            case 1:
                // light_sleep
                return ActivityKind.TYPE_LIGHT_SLEEP;
            case 2:
                // awake
            case 3:
                // more_awake
                return ActivityKind.TYPE_ACTIVITY;
            default:
                // ?
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_NOT_WORN:
                return RAW_NOT_WORN;

            case ActivityKind.TYPE_ACTIVITY:
                // generic
                //noinspection PointlessBitwiseExpression
                return RAW_TYPE_KIND_ACTIVITY | 0;
            case ActivityKind.TYPE_RUNNING:
                // running
                return RAW_TYPE_KIND_ACTIVITY | 1;
            case ActivityKind.TYPE_CYCLING:
                // cycling
                return RAW_TYPE_KIND_ACTIVITY | 2;
            case ActivityKind.TYPE_ACTIVITY | ActivityKind.TYPE_RUNNING | ActivityKind.TYPE_WALKING | ActivityKind.TYPE_EXERCISE | ActivityKind.TYPE_SWIMMING:
                return RAW_TYPE_KIND_ACTIVITY | 3;
            case ActivityKind.TYPE_EXERCISE:
                // fitness_equipment
                return RAW_TYPE_KIND_ACTIVITY | 4;
            case ActivityKind.TYPE_SWIMMING:
                // swimming
                return RAW_TYPE_KIND_ACTIVITY | 5;
            case ActivityKind.TYPE_WALKING:
                // walking
                return RAW_TYPE_KIND_ACTIVITY | 6;
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return RAW_TYPE_KIND_SLEEP | 1;
            case ActivityKind.TYPE_DEEP_SLEEP:
                //noinspection PointlessBitwiseExpression
                return RAW_TYPE_KIND_SLEEP | 0;
            default:
                if ((activityKind & ActivityKind.TYPE_SWIMMING) != 0) return RAW_TYPE_KIND_ACTIVITY | 5;
                if ((activityKind & ActivityKind.TYPE_CYCLING) != 0) return RAW_TYPE_KIND_ACTIVITY | 2;
                if ((activityKind & ActivityKind.TYPE_RUNNING) != 0) return RAW_TYPE_KIND_ACTIVITY | 1;
                if ((activityKind & ActivityKind.TYPE_EXERCISE) != 0) return RAW_TYPE_KIND_ACTIVITY | 4;
                if ((activityKind & ActivityKind.TYPE_WALKING) != 0) return RAW_TYPE_KIND_ACTIVITY | 6;
                if ((activityKind & ActivityKind.TYPE_SLEEP) != 0) return RAW_TYPE_KIND_SLEEP | 1;
                if ((activityKind & ActivityKind.TYPE_ACTIVITY) != 0) {
                    //noinspection PointlessBitwiseExpression
                    return RAW_TYPE_KIND_ACTIVITY | 0;
                }
                return 0;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / 255.0f;
    }

    @Override
    public VivomoveHrActivitySample createActivitySample() {
        return new VivomoveHrActivitySample();
    }

    @Override
    public AbstractDao<VivomoveHrActivitySample, ?> getSampleDao() {
        return getSession().getVivomoveHrActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return VivomoveHrActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return VivomoveHrActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return VivomoveHrActivitySampleDao.Properties.DeviceId;
    }

    public static String rawKindToString(int rawType) {
        if (rawType == RAW_NOT_WORN) {
            return "not worn";
        }

        switch (rawType & RAW_TYPE_KIND_MASK) {
            case RAW_TYPE_KIND_ACTIVITY:
                return activityTypeToString(rawType & ~RAW_TYPE_KIND_MASK);
            case RAW_TYPE_KIND_SLEEP:
                return sleepTypeToString(rawType & ~RAW_TYPE_KIND_MASK);
            default:
                // ???
                return "unknown " + rawType;
        }
    }

    private static String activityTypeToString(int rawType) {
        switch (rawType) {
            case 0:
                return "generic";
            case 1:
                return "running";
            case 2:
                return "cycling";
            case 3:
                return "transition";
            case 4:
                return "fitness equipment";
            case 5:
                return "swimming";
            case 6:
                return "walking";
            case 8:
                return "sedentary";
            default:
                return "unknown activity " + rawType;
        }
    }

    private static String sleepTypeToString(int rawType) {
        switch (rawType) {
            case 0:
                return "deep sleep";
            case 1:
                return "light sleep";
            case 2:
                // awake
                return "awake";
            case 3:
                // more_awake
                return "more awake";
            default:
                return "unknown sleep " + rawType;
        }
    }
}

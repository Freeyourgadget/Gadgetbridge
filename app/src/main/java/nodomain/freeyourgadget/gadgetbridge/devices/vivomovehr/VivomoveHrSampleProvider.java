/*  Copyright (C) 2023-2024 Petr Kadlec

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
    public ActivityKind normalizeType(int rawType) {
        if (rawType == RAW_NOT_WORN) {
            return ActivityKind.NOT_WORN;
        }

        switch (rawType & RAW_TYPE_KIND_MASK) {
            case RAW_TYPE_KIND_ACTIVITY:
                return normalizeActivityType(rawType & ~RAW_TYPE_KIND_MASK);
            case RAW_TYPE_KIND_SLEEP:
                return normalizeSleepType(rawType & ~RAW_TYPE_KIND_MASK);
            default:
                // ???
                return ActivityKind.UNKNOWN;
        }
    }

    private static ActivityKind normalizeActivityType(int rawType) {
        switch (rawType) {
            case 0:
                // generic
                return ActivityKind.ACTIVITY;
            case 1:
                // running
                return ActivityKind.RUNNING;
            case 2:
                // cycling
                return ActivityKind.CYCLING;
            case 3:
                // transition
                return ActivityKind.VIVOMOVE_HR_TRANSITION;
            case 4:
                // fitness_equipment
                return ActivityKind.EXERCISE;
            case 5:
                // swimming
                return ActivityKind.SWIMMING;
            case 6:
                // walking
                return ActivityKind.WALKING;
            case 8:
                // sedentary
                // TODO?
                return ActivityKind.ACTIVITY;

            default:
                return ActivityKind.UNKNOWN;
        }
    }

    private static ActivityKind normalizeSleepType(int rawType) {
        switch (rawType) {
            case 0:
                // deep_sleep
                return ActivityKind.DEEP_SLEEP;
            case 1:
                // light_sleep
                return ActivityKind.LIGHT_SLEEP;
            case 2:
                // awake
            case 3:
                // more_awake
                return ActivityKind.ACTIVITY;
            default:
                // ?
                return ActivityKind.UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        switch (activityKind) {
            case NOT_WORN:
                return RAW_NOT_WORN;

            case ACTIVITY:
                // generic
                //noinspection PointlessBitwiseExpression
                return RAW_TYPE_KIND_ACTIVITY | 0;
            case RUNNING:
                // running
                return RAW_TYPE_KIND_ACTIVITY | 1;
            case CYCLING:
                // cycling
                return RAW_TYPE_KIND_ACTIVITY | 2;
            case VIVOMOVE_HR_TRANSITION:
                return RAW_TYPE_KIND_ACTIVITY | 3;
            case EXERCISE:
                // fitness_equipment
                return RAW_TYPE_KIND_ACTIVITY | 4;
            case SWIMMING:
                // swimming
                return RAW_TYPE_KIND_ACTIVITY | 5;
            case WALKING:
                // walking
                return RAW_TYPE_KIND_ACTIVITY | 6;
            case LIGHT_SLEEP:
                return RAW_TYPE_KIND_SLEEP | 1;
            case DEEP_SLEEP:
                //noinspection PointlessBitwiseExpression
                return RAW_TYPE_KIND_SLEEP | 0;
            default:
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

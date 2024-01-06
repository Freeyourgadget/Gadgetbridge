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
package nodomain.freeyourgadget.gadgetbridge.model;

public class ActivitySummaryEntries {
    public static final String TIME_START = "startTime";
    public static final String TIME_END = "endTime";
    public static final String ACTIVE_SECONDS = "activeSeconds";

    public static final String ALTITUDE_AVG = "averageAltitude";
    public static final String ALTITUDE_BASE = "baseAltitude";
    public static final String ALTITUDE_MAX = "maxAltitude";
    public static final String ALTITUDE_MIN = "minAltitude";

    public static final String ASCENT_DISTANCE = "ascentDistance";
    public static final String ASCENT_METERS = "ascentMeters";
    public static final String ASCENT_SECONDS = "ascentSeconds";
    public static final String DESCENT_DISTANCE = "descentDistance";
    public static final String DESCENT_METERS = "descentMeters";
    public static final String DESCENT_SECONDS = "descentSeconds";
    public static final String FLAT_DISTANCE = "flatDistance";
    public static final String FLAT_SECONDS = "flatSeconds";

    public static final String CADENCE_AVG = "averageCadence";
    public static final String CADENCE_MAX = "maxCadence";
    public static final String CADENCE_MIN = "minCadence";

    public static final String SPEED_AVG = "averageSpeed";
    public static final String SPEED_MAX = "maxSpeed";
    public static final String SPEED_MIN = "minSpeed";

    public static final String DISTANCE_METERS = "distanceMeters";
    public static final String ELEVATION_GAIN = "elevationGain";
    public static final String ELEVATION_LOSS = "elevationLoss";

    public static final String HR_AVG = "averageHR";
    public static final String HR_MAX = "maxHR";
    public static final String HR_MIN = "minHR";
    public static final String HR_ZONE_NA = "hrZoneNa";
    public static final String HR_ZONE_WARM_UP = "hrZoneWarmUp";
    public static final String HR_ZONE_FAT_BURN = "hrZoneFatBurn";
    public static final String HR_ZONE_AEROBIC = "hrZoneAerobic";
    public static final String HR_ZONE_ANAEROBIC = "hrZoneAnaerobic";
    public static final String HR_ZONE_EXTREME = "hrZoneExtreme";

    public static final String LANE_LENGTH = "laneLength";
    public static final String LAPS = "laps";
    public static final String LAP_PACE_AVERAGE = "averageLapPace";

    public static final String PACE_AVG_SECONDS_KM = "averageKMPaceSeconds";
    public static final String PACE_MAX = "maxPace";
    public static final String PACE_MIN = "minPace";
    public static final String STEPS = "steps";
    public static final String STRIDE_AVG = "averageStride";
    public static final String STRIDE_MAX = "maxStride";
    public static final String STRIDE_MIN = "minStride";
    public static final String STRIDE_TOTAL = "totalStride";

    public static final String STROKE_DISTANCE_AVG = "averageStrokeDistance";
    public static final String STROKE_AVG_PER_SECOND = "averageStrokesPerSecond";
    public static final String STROKE_RATE_AVG = "avgStrokeRate";
    public static final String STROKE_RATE_MAX = "maxStrokeRate";
    public static final String STROKES = "strokes";

    public static final String SWIM_STYLE = "swimStyle";
    public static final String SWOLF_INDEX = "swolfIndex";

    public static final String CALORIES_BURNT = "caloriesBurnt";
    public static final String TRAINING_EFFECT_AEROBIC = "aerobicTrainingEffect";
    public static final String TRAINING_EFFECT_ANAEROBIC = "anaerobicTrainingEffect";
    public static final String WORKOUT_LOAD = "currentWorkoutLoad";
    public static final String MAXIMUM_OXYGEN_UPTAKE = "maximumOxygenUptake";
    public static final String RECOVERY_TIME = "recoveryTime";

    public static final String UNIT_BPM = "bpm";
    public static final String UNIT_CM = "cm";
    public static final String UNIT_UNIX_EPOCH_SECONDS = "unix_epoch_seconds";
    public static final String UNIT_KCAL = "calories_unit";
    public static final String UNIT_LAPS = "laps_unit";
    public static final String UNIT_METERS = "meters";
    public static final String UNIT_ML_KG_MIN = "ml/kg/min";
    public static final String UNIT_NONE = "";
    public static final String UNIT_HOURS = "hours";
    public static final String UNIT_SECONDS = "seconds";
    public static final String UNIT_SECONDS_PER_KM = "seconds_km";
    public static final String UNIT_SECONDS_PER_M = "seconds_m";
    public static final String UNIT_METERS_PER_SECOND = "meters_second";
    public static final String UNIT_KMPH = "km_h";
    public static final String UNIT_SPM = "spm";
    public static final String UNIT_STEPS = "steps_unit";
    public static final String UNIT_STROKES = "strokes_unit";
    public static final String UNIT_STROKES_PER_MINUTE = "strokes_minute";
    public static final String UNIT_STROKES_PER_SECOND = "strokes_second";
    public static final String UNIT_YARD = "yard";
}

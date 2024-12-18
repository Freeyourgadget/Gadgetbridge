/*  Copyright (C) 2024 José Rebelo

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

public class ActivitySummaryEntries {
    public static final String STATUS = "status";
    public static final String TYPE = "watchface_dialog_widget_type"; // TODO: change this?

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

    public static final String STEP_RATE_SUM = "stepRateSum";
    public static final String STEP_RATE_AVG = "stepRateAvg";
    public static final String STEP_RATE_MAX = "stepRateMax";
    public static final String STEP_LENGTH_AVG = "stepLengthAvg";

    public static final String CADENCE_AVG = "averageCadence";
    public static final String CADENCE_MAX = "maxCadence";
    public static final String CADENCE_MIN = "minCadence";

    public static final String SPEED_AVG = "averageSpeed";
    public static final String SPEED_MAX = "maxSpeed";
    public static final String SPEED_MIN = "minSpeed";

    public static final String GROUND_CONTACT_TIME_AVG = "groundContactTimeAvg";
    public static final String IMPACT_AVG = "impactAvg";
    public static final String IMPACT_MAX = "impactMax";
    public static final String SWING_ANGLE_AVG = "swingAngleAvg";
    public static final String FORE_FOOT_LANDINGS = "foreFootLandings";
    public static final String MID_FOOT_LANDINGS = "midFootLandings";
    public static final String BACK_FOOT_LANDINGS = "backFootLandings";
    public static final String EVERSION_ANGLE_AVG = "eversionAngleAvg";
    public static final String EVERSION_ANGLE_MAX = "eversionAngleMax";

    public static final String DISTANCE_METERS = "distanceMeters";
    public static final String ELEVATION_GAIN = "elevationGain";
    public static final String ELEVATION_LOSS = "elevationLoss";

    public static final String HR_AVG = "averageHR";
    public static final String HR_MAX = "maxHR";
    public static final String HR_MIN = "minHR";
    public static final String HR_ZONE_NA = "hrZoneNa";
    public static final String HR_ZONE_WARM_UP = "hrZoneWarmUp";
    public static final String HR_ZONE_EASY = "hrZoneEasy";
    public static final String HR_ZONE_FAT_BURN = "hrZoneFatBurn";
    public static final String HR_ZONE_AEROBIC = "hrZoneAerobic";
    public static final String HR_ZONE_ANAEROBIC = "hrZoneAnaerobic";
    public static final String HR_ZONE_THRESHOLD = "hrZoneThreshold";
    public static final String HR_ZONE_EXTREME = "hrZoneExtreme";
    public static final String HR_ZONE_MAXIMUM = "hrZoneMaximum";

    public static final String RESPIRATION_AVG = "average_respiration_rate";
    public static final String RESPIRATION_MAX = "max_respiration_rate";
    public static final String RESPIRATION_MIN = "min_respiration_rate";
    public static final String SPO2_AVG = "menuitem_spo2";
    public static final String STRESS_AVG = "menuitem_stress";
    public static final String HRV_SDRR = "hrv_sdrr";
    public static final String HRV_RMSSD = "hrv_rmssd";

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

    public static final String JUMP_RATE_AVG = "avgJumpRate";
    public static final String JUMP_RATE_MAX = "maxJumpRate";
    public static final String JUMPS = "totalJumps";

    public static final String SWIM_STYLE = "swimStyle";
    public static final String SWOLF_INDEX = "swolfIndex";
    public static final String SWOLF_AVG = "swolfAvg";
    public static final String SWOLF_MAX = "swolfMax";
    public static final String SWOLF_MIN = "swolfMin";

    public static final String CALORIES_BURNT = "caloriesBurnt";
    public static final String TRAINING_EFFECT_AEROBIC = "aerobicTrainingEffect";
    public static final String TRAINING_EFFECT_ANAEROBIC = "anaerobicTrainingEffect";
    public static final String WORKOUT_LOAD = "currentWorkoutLoad";
    public static final String MAXIMUM_OXYGEN_UPTAKE = "maximumOxygenUptake";
    public static final String RECOVERY_TIME = "recoveryTime";
    public static final String ESTIMATED_SWEAT_LOSS = "estimatedSweatLoss";
    public static final String LACTATE_THRESHOLD_HR = "lactateThresholdHeartRate";

    public static final String CYCLING_POWER_AVERAGE = "cyclingPowerAverage";
    public static final String CYCLING_POWER_MIN = "cyclingPowerMin";
    public static final String CYCLING_POWER_MAX = "cyclingPowerMax";

    public static final String SETS = "workoutSets";

    public static final String UNIT_BPM = "bpm";
    public static final String UNIT_BREATHS_PER_MIN = "breaths_per_min";
    public static final String UNIT_CM = "cm";
    public static final String UNIT_UNIX_EPOCH_SECONDS = "unix_epoch_seconds";
    public static final String UNIT_KCAL = "calories_unit";
    public static final String UNIT_ML = "ml";
    public static final String UNIT_LAPS = "laps_unit";
    public static final String UNIT_KILOMETERS = "km";
    public static final String UNIT_METERS = "meters";
    public static final String UNIT_PERCENTAGE = "%";
    public static final String UNIT_ML_KG_MIN = "ml/kg/min";
    public static final String UNIT_NONE = "";
    public static final String UNIT_HOURS = "hours";
    public static final String UNIT_SECONDS = "seconds";
    public static final String UNIT_MILLISECONDS = "milliseconds_ms";
    public static final String UNIT_SECONDS_PER_KM = "seconds_km";
    public static final String UNIT_SECONDS_PER_M = "seconds_m";
    public static final String UNIT_METERS_PER_SECOND = "meters_second";
    public static final String UNIT_KMPH = "km_h";
    public static final String UNIT_SPM = "spm";
    public static final String UNIT_STEPS = "steps_unit";
    public static final String UNIT_STROKES = "strokes_unit";
    public static final String UNIT_STROKES_PER_MINUTE = "strokes_minute";
    public static final String UNIT_STROKES_PER_SECOND = "strokes_second";
    public static final String UNIT_JUMPS = "jumps_unit";
    public static final String UNIT_JUMPS_PER_MINUTE = "jumps_minute";
    public static final String UNIT_YARD = "yard";
    public static final String UNIT_DEGREES = "degrees";
    public static final String UNIT_STRING = "string";
    public static final String UNIT_RAW_STRING = "raw_string";
    public static final String UNIT_KG = "kg";

    public static final String GROUP_PACE = "Pace";
    public static final String GROUP_ACTIVITY = "Activity";
    public static final String GROUP_SPEED = "Speed";
    public static final String GROUP_ELEVATION = "Elevation";
    public static final String GROUP_HEART_RATE_ZONES = "HeartRateZones";
    public static final String GROUP_STROKES = "Strokes";
    public static final String GROUP_JUMPS = "Jumps";
    public static final String GROUP_SWIMMING = "Swimming";
    public static final String GROUP_TRAINING_EFFECT = "TrainingEffect";
    public static final String GROUP_LAPS = "laps";
    public static final String GROUP_RUNNING_FORM = "RunningForm";

    /**
     * Used to signal that this activity has a gps track. This is currently used by ActivitySummaryDetail
     * to display the share and view gpx buttons, even though there's no gpx file.
     * FIXME: We should have a cleaner way of doing this.
     */
    public static final String INTERNAL_HAS_GPS = "internal_hasGps";
}

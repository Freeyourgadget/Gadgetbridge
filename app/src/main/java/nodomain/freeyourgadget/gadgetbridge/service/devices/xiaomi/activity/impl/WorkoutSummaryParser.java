/*  Copyright (C) 2023-2024 José Rebelo, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ALTITUDE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ALTITUDE_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ALTITUDE_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CADENCE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CALORIES_BURNT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ELEVATION_GAIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ELEVATION_LOSS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_AEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_ANAEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_EXTREME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_FAT_BURN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_WARM_UP;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.LAPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.LAP_PACE_AVERAGE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.MAXIMUM_OXYGEN_UPTAKE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.PACE_AVG_SECONDS_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.PACE_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.PACE_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.RECOVERY_TIME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SPEED_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SPEED_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STEP_RATE_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STROKES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STROKE_RATE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.JUMPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.JUMP_RATE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.JUMP_RATE_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SWIM_STYLE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SWOLF_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_END;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_START;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_HOURS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KCAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KMPH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_LAPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_ML_KG_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NONE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_M;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_STROKES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_STROKES_PER_MINUTE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_JUMPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_JUMPS_PER_MINUTE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_UNIX_EPOCH_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.WORKOUT_LOAD;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl.XiaomiSimpleActivityParser.XIAOMI_WORKOUT_TYPE;

import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class WorkoutSummaryParser extends XiaomiActivityParser implements ActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutSummaryParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        BaseActivitySummary summary = new BaseActivitySummary();

        summary.setStartTime(fileId.getTimestamp()); // due to a bug this has to be set
        summary.setEndTime(fileId.getTimestamp()); // due to a bug this has to be set
        summary.setActivityKind(ActivityKind.UNKNOWN.getCode());
        summary.setRawSummaryData(bytes);

        try {
            summary = parseBinaryData(summary, true);
        } catch (final Exception e) {
            LOG.error("Failed to parse workout summary", e);
            GB.toast(support.getContext(), "Failed to parse workout summary", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        // parseBinaryData may return null in case the version is not supported
        if (summary == null) {
            LOG.warn("summary is null - should never happen {}", fileId);
            return false;
        }

        summary.setSummaryData(null); // remove json before saving to database

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(support.getDevice(), session);
            final User user = DBHelper.getUser(session);

            final BaseActivitySummary existingSummary = findOrCreateBaseActivitySummary(session, device, user, fileId);
            existingSummary.setEndTime(summary.getEndTime());
            existingSummary.setActivityKind(summary.getActivityKind());
            existingSummary.setRawSummaryData(summary.getRawSummaryData());
            existingSummary.setSummaryData(null);  // remove json before saving to database

            session.getBaseActivitySummaryDao().insertOrReplace(existingSummary);
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving activity summary", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    @Override
    public BaseActivitySummary parseBinaryData(final BaseActivitySummary summary, final boolean forDetails) {
        final byte[] data = summary.getRawSummaryData();
        if (data == null) {
            return summary;
        }

        final int arrCrc32 = CheckSums.getCRC32(data, 0, data.length - 4);
        final int expectedCrc32 = BLETypeConversions.toUint32(data, data.length - 4);

        final ByteBuffer buf;
        if (arrCrc32 != expectedCrc32) {
            // If the CRC32 is not valid, we're missing 1 header padding byte due to a previous bug
            // This previous version also did not include the CRC at the end
            // More info: https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/3916
            buf = ByteBuffer.allocate(data.length + 1).order(ByteOrder.LITTLE_ENDIAN);
            buf.put(data, 0, 7);
            buf.put((byte) 0);
            buf.put(data, 7, data.length - 7);
            buf.flip();
        } else {
            // Valid full file, skip crc
            buf = ByteBuffer.wrap(data, 0, data.length - 4).order(ByteOrder.LITTLE_ENDIAN);
        }

        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(buf);

        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }

        XiaomiSimpleActivityParser parser = null;

        switch (fileId.getSubtype()) {
            case SPORTS_OUTDOOR_WALKING_V1:
                summary.setActivityKind(ActivityKind.WALKING.getCode());
                parser = getOutdoorWalkingV1Parser(fileId);
                break;
            case SPORTS_OUTDOOR_RUNNING:
                summary.setActivityKind(ActivityKind.RUNNING.getCode());
                parser = getOutdoorWalkingV1Parser(fileId);
                break;
            case SPORTS_INDOOR_CYCLING:
                summary.setActivityKind(ActivityKind.INDOOR_CYCLING.getCode());
                parser = getIndoorCyclingParser(fileId);
                break;
            case SPORTS_FREESTYLE:
                summary.setActivityKind(ActivityKind.EXERCISE.getCode());
                parser = getFreestyleParser(fileId);
                break;
            case SPORTS_POOL_SWIMMING:
                summary.setActivityKind(ActivityKind.SWIMMING.getCode());
                parser = getPoolSwimmingParser(fileId);
                break;
            case SPORTS_HIIT:
                summary.setActivityKind(ActivityKind.HIIT.getCode());
                parser = getHiitParser(fileId);
                break;
            case SPORTS_ELLIPTICAL:
                summary.setActivityKind(ActivityKind.ELLIPTICAL_TRAINER.getCode());
                parser = getEllipticalParser(fileId);
                break;
            case SPORTS_OUTDOOR_WALKING_V2:
                parser = getOutdoorWalkingV2Parser(fileId);
                break;
            case SPORTS_OUTDOOR_CYCLING_V2:
                summary.setActivityKind(ActivityKind.OUTDOOR_CYCLING.getCode());
                parser = getOutdoorCyclingV2Parser(fileId);
                break;
            case SPORTS_OUTDOOR_CYCLING:
                parser = getOutdoorCyclingParser(fileId);
                break;
            case SPORTS_TREADMILL:
                summary.setActivityKind(ActivityKind.TREADMILL.getCode());
                parser = getTreadmillParser(fileId);
                break;
            case SPORTS_ROWING:
                summary.setActivityKind(ActivityKind.ROWING.getCode());
                parser = getRowingParser(fileId);
                break;
            case SPORTS_JUMP_ROPING:
                summary.setActivityKind(ActivityKind.JUMP_ROPING.getCode());
                parser = getJumpRopingParser(fileId);
                break;
            default:
                LOG.warn("No workout summary parser for {}", fileId.getSubtypeCode());
                break;
        }

        if (parser != null) {
            parser.parse(summary, buf);
        }

        return summary;
    }

    @Nullable
    private XiaomiSimpleActivityParser getFreestyleParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 5:   // for Smart Band 8 Active
                headerSize = 3;
                break;
            case 8:
            case 9:
            case 10:
                headerSize = 6;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        if (version == 5) {
            builder.addUnknown(7); // probably training effect aerobic and recovery time?
        } else {
            builder.addUnknown(6);
            builder.addFloat(TRAINING_EFFECT_AEROBIC, UNIT_NONE);
            builder.addUnknown(1);
            builder.addUnknown(1);
            builder.addShort(RECOVERY_TIME, UNIT_HOURS);
        }
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
        if (version == 5) {
            builder.addUnknown(10); // Probably the same as for v8 below?
            builder.addShort(XIAOMI_WORKOUT_TYPE, XIAOMI_WORKOUT_TYPE);
            builder.addUnknown(2);
            builder.addInt("configuredTimeGoal", UNIT_SECONDS);
            builder.addShort("configuredCaloriesGoal", UNIT_KCAL);
        } else {
            builder.addUnknown(2);
            builder.addUnknown(4); // activeSeconds again?, UNIT_SECONDS
            builder.addFloat(TRAINING_EFFECT_ANAEROBIC, UNIT_NONE);
            builder.addUnknown(1);
            builder.addShort(XIAOMI_WORKOUT_TYPE, XIAOMI_WORKOUT_TYPE);
            builder.addUnknown(2);
            builder.addInt("configuredTimeGoal", UNIT_SECONDS);
            builder.addShort("configuredCaloriesGoal", UNIT_KCAL);
            builder.addShort(WORKOUT_LOAD, UNIT_NONE);
            builder.addUnknown(1);
            builder.addByte("vitality_gain", UNIT_NONE);
        }
        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getIndoorCyclingParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 8:
                headerSize = 7;
                break;
            case 9:
                headerSize = 8;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addUnknown(4);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addUnknown(4);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addFloat(TRAINING_EFFECT_AEROBIC, UNIT_NONE);
        builder.addUnknown(1);
        builder.addUnknown(1);
        builder.addShort(RECOVERY_TIME, UNIT_HOURS);
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
        builder.addUnknown(2);
        builder.addUnknown(4);
        builder.addFloat(TRAINING_EFFECT_ANAEROBIC, UNIT_NONE);
        // FIXME identify field lengths to align with the header
        builder.addUnknown(3);
        builder.addInt("configuredTimeGoal", UNIT_SECONDS);
        builder.addShort("configuredCaloriesGoal", UNIT_KCAL);
        builder.addShort("maximumCaloriesGoal", UNIT_KCAL);  // TODO: mhm?
        builder.addUnknown(28);
        builder.addShort(WORKOUT_LOAD, UNIT_NONE); // training load
        builder.addUnknown(24);
        builder.addByte("configuredSets", UNIT_NONE);
        builder.addUnknown(13);
        builder.addInt("startTime2", UNIT_SECONDS);
        builder.addInt("endTime2", UNIT_SECONDS);
        builder.addInt("goal", UNIT_NONE); // TODO match against goalType
        builder.addInt("duration2", UNIT_SECONDS);
        builder.addInt("intervalTime", UNIT_SECONDS);
        builder.addUnknown(56);
        builder.addInt("hrZoneExtreme2", UNIT_SECONDS);
        builder.addInt("hrZoneAnaerobic2", UNIT_SECONDS);
        builder.addInt("hrZoneAerobic2", UNIT_SECONDS);
        builder.addInt("hrZoneFatBurn2", UNIT_SECONDS);
        builder.addInt("hrZoneWarmUp2", UNIT_SECONDS);
        builder.addUnknown(16);
        builder.addShort("vitality_gain", UNIT_NONE);
        builder.addShort("training_load2", UNIT_NONE);
        builder.addShort("recovery_time2", UNIT_HOURS);

        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getOutdoorWalkingV1Parser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 4:
                headerSize = 4;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addInt(DISTANCE_METERS, UNIT_METERS);
        builder.addInt(CALORIES_BURNT, UNIT_KCAL);
        builder.addInt(PACE_MAX, UNIT_SECONDS_PER_M);
        builder.addInt(PACE_MIN, UNIT_SECONDS_PER_M);
        builder.addUnknown(4);
        builder.addInt(STEPS, UNIT_STEPS);
        builder.addUnknown(2); // pace?
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        // FIXME identify field lengths to align with the header
        builder.addUnknown(20);
        builder.addFloat("recoveryValue", "recoveryValue");
        builder.addUnknown(9);
        builder.addByte(RECOVERY_TIME, UNIT_SECONDS);
        builder.addUnknown(2);
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
        builder.addInt("configured_time_goal", UNIT_SECONDS);

        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getOutdoorWalkingV2Parser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 1:  // for Smart Band 8 Active
                headerSize = 5;
                break;
            case 4:
                headerSize = 7;
                break;
            case 5:
            case 6:
                headerSize = 9;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }
        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addShort(XIAOMI_WORKOUT_TYPE, XIAOMI_WORKOUT_TYPE);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addUnknown(4);
        builder.addInt(DISTANCE_METERS, UNIT_METERS);
        builder.addUnknown(2);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        if (version == 1) {
            builder.addInt(PACE_MAX, UNIT_SECONDS_PER_KM);
            builder.addInt(PACE_MIN, UNIT_SECONDS_PER_KM);
            builder.addFloat(SPEED_MAX, UNIT_KMPH);
            builder.addInt(STEPS, UNIT_STEPS);
            builder.addShort(STEP_RATE_MAX, UNIT_SPM);					   
            builder.addByte(HR_AVG, UNIT_BPM);
            builder.addByte(HR_MAX, UNIT_BPM);
            builder.addByte(HR_MIN, UNIT_BPM);
            builder.addUnknown(33);
            builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
            builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
            builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
            builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
            builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
            builder.addUnknown(21);
        } else {
            if (version >= 5) {
                builder.addInt(PACE_AVG_SECONDS_KM, UNIT_SECONDS_PER_KM);
            }
            builder.addInt(PACE_MAX, UNIT_SECONDS_PER_KM);
            builder.addInt(PACE_MIN, UNIT_SECONDS_PER_KM);
            if (version >= 5) {
                builder.addFloat(SPEED_AVG, UNIT_KMPH);
            }
            builder.addFloat(SPEED_MAX, UNIT_KMPH);
            builder.addInt(STEPS, UNIT_STEPS);
            if (version >= 5) {
                builder.addUnknown(4);
            }
            builder.addShort(STEP_RATE_MAX, UNIT_SPM);
            builder.addByte(HR_AVG, UNIT_BPM);
            builder.addByte(HR_MAX, UNIT_BPM);
            builder.addByte(HR_MIN, UNIT_BPM);
            builder.addUnknown(20);
            builder.addFloat(TRAINING_EFFECT_AEROBIC, UNIT_NONE);
            builder.addUnknown(1);
            builder.addFloat(TRAINING_EFFECT_ANAEROBIC, UNIT_NONE);
            builder.addUnknown(4);
            builder.addByte(RECOVERY_TIME, UNIT_HOURS);
            builder.addUnknown(2);
            builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
            builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
            builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
            builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
            builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
            builder.addInt("configuredTimeGoal", UNIT_SECONDS);
            builder.addShort("configuredCaloriesGoal", UNIT_KCAL);
            builder.addInt("configuredDistanceGoal", UNIT_METERS);
            builder.addUnknown(11);
            builder.addShort(WORKOUT_LOAD, UNIT_NONE); // training load
            builder.addUnknown(7);
            builder.addByte("vitality_gain", UNIT_NONE);
            builder.addUnknown(16);
            builder.addUnknown(1); // HR_AVG duplicate
            builder.addUnknown(1); // HR_MAX duplicate
            builder.addUnknown(1); // HR_MIN duplicate
            builder.addUnknown(2);
            builder.addByte(CADENCE_AVG, UNIT_SPM);
        }
        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getOutdoorCyclingParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 4:
            case 5:
                headerSize = 6;
                break;
            case 6:
                headerSize = 7;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addShort(XIAOMI_WORKOUT_TYPE, XIAOMI_WORKOUT_TYPE);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addUnknown(4);
        builder.addInt(DISTANCE_METERS, UNIT_METERS);
        builder.addUnknown(2);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addUnknown(4);
        builder.addUnknown(4);
        if (version >= 5) {
            builder.addFloat(SPEED_AVG, UNIT_KMPH);
        }
        builder.addFloat(SPEED_MAX, UNIT_KMPH);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addFloat(ELEVATION_GAIN, UNIT_METERS);
        builder.addFloat(ELEVATION_LOSS, UNIT_METERS);
        builder.addFloat(ALTITUDE_AVG, UNIT_METERS);
        builder.addFloat(ALTITUDE_MAX, UNIT_METERS);
        builder.addFloat(ALTITUDE_MIN, UNIT_METERS);
        builder.addFloat(TRAINING_EFFECT_AEROBIC, UNIT_NONE);
        builder.addUnknown(1);
        builder.addFloat(TRAINING_EFFECT_ANAEROBIC, UNIT_NONE);
        builder.addUnknown(1);
        builder.addByte(MAXIMUM_OXYGEN_UPTAKE, UNIT_ML_KG_MIN);
        builder.addUnknown(1);
        builder.addUnknown(1);
        builder.addShort(RECOVERY_TIME, UNIT_HOURS);
        builder.addUnknown(1);
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);

        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getHiitParser(final XiaomiActivityFileId fileId){
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 5:
                headerSize = 4;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addFloat(TRAINING_EFFECT_AEROBIC, UNIT_NONE);
        builder.addUnknown(1);
        builder.addUnknown(1);
        builder.addShort(RECOVERY_TIME, UNIT_HOURS);
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);

        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getPoolSwimmingParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 6:
                headerSize = 4;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addInt(DISTANCE_METERS, UNIT_METERS);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addUnknown(11);
        builder.addShort(STROKES, UNIT_STROKES);
        builder.addByte(SWIM_STYLE, UNIT_NONE);
        builder.addUnknown(1);
        builder.addShort(LAPS, UNIT_LAPS);
        builder.addShort(SWOLF_AVG, UNIT_NONE);
        builder.addUnknown(2); // minSWOLF
        builder.addByte("configuredLaneLength", UNIT_METERS);
        builder.addUnknown(6);
        // builder.addInt("activeSec2", UNIT_SECONDS);
        builder.addInt("configuredTimeGoal", UNIT_SECONDS);
        builder.addShort("configuredCaloriesGoal", UNIT_KCAL);
        builder.addUnknown(8);
        builder.addByte("configuredLengthsGoal", UNIT_NONE);
        builder.addUnknown(14);
        builder.addByte("vitality_gain", UNIT_NONE);
        builder.addUnknown(60);
        // builder.addInt("startingTime2", UNIT_UNIX_EPOCH_SECONDS);
        // builder.addInt("endTime2", UNIT_UNIX_EPOCH_SECONDS);
        // builder.addUnknown(4);
        // builder.addInt("activeSec3", UNIT_SECONDS);
        // builder.addInt("activeSec4", UNIT_SECONDS);
        // builder.addUnknown(8);
        // builder.addInt("caloriesBurnt2", UNIT_KCAL);
        // builder.addInt("distanceMeter2", UNIT_METERS);
        // builder.addUnknown(4);
        builder.addInt(LAP_PACE_AVERAGE, UNIT_SECONDS);
        builder.addInt(PACE_MAX, UNIT_SECONDS); // not confirmed
        builder.addUnknown(4);
        builder.addInt(STROKE_RATE_AVG, UNIT_STROKES_PER_MINUTE);
        builder.addUnknown(14);
        // builder.addInt("laps2", UNIT_LAPS);
        // builder.addShort("avgSWOLF2", UNIT_NONE);
        builder.addShort("minSWOLF", UNIT_NONE);
        builder.addShort("maxSWOLF", UNIT_NONE);
        // builder.addInt("totalStrokes2", UNIT_STROKES);

        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getEllipticalParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 3:
                headerSize = 4;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addInt(STEPS, UNIT_STEPS);
        builder.addUnknown(2);        // MAX_STEPS_PER_MINUTE, UNIT_STEPS_PER_MINUTE
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addUnknown(7);
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
        builder.addUnknown(20);

        return builder.build();
    }
    
    @Nullable
    private XiaomiSimpleActivityParser getRowingParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 4:
                headerSize = 4;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addUnknown(7);
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
        builder.addUnknown(2);
        builder.addInt(STROKES, UNIT_STROKES);
        builder.addInt(STROKE_RATE_AVG, UNIT_STROKES_PER_MINUTE);
        builder.addUnknown(4);   // STROKE_RATE_MAX, UNIT_STROKES_PER_MINUTE
        builder.addUnknown(31);
        return builder.build();
    }    
    
    @Nullable
    private XiaomiSimpleActivityParser getTreadmillParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 5:
                headerSize = 4;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addInt(DISTANCE_METERS, UNIT_METERS);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addUnknown(4);
        builder.addUnknown(4);
        builder.addInt(STEPS, UNIT_STEPS);
        builder.addUnknown(2);        // MAX_STEPS_PER_MINUTE, UNIT_STEPS_PER_MINUTE
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addUnknown(8);
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
        builder.addUnknown(32);

        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getOutdoorCyclingV2Parser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 4:
                headerSize = 5;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addInt(DISTANCE_METERS, UNIT_METERS);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addUnknown(8);
        builder.addFloat(SPEED_MAX, UNIT_KMPH);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addUnknown(28);
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
        builder.addUnknown(18);
        builder.addUnknown(2); // configuredTimeGoal, UNIT_SECONDS
        builder.addUnknown(6);

        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getJumpRopingParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 3:
            case 5:
                headerSize = 5;
                break;
            default:
                LOG.warn("Unable to parse workout summary version {}", fileId.getVersion());
                return null;
        }

        final XiaomiSimpleActivityParser.Builder builder = new XiaomiSimpleActivityParser.Builder();
        builder.setHeaderSize(headerSize);
        builder.addInt(TIME_START, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(TIME_END, UNIT_UNIX_EPOCH_SECONDS);
        builder.addInt(ACTIVE_SECONDS, UNIT_SECONDS);
        builder.addShort(CALORIES_BURNT, UNIT_KCAL);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addFloat(TRAINING_EFFECT_AEROBIC, UNIT_NONE);
        if (version == 3) {
            builder.addUnknown(3); // unknown byte and recovery time?
        } else {
            builder.addUnknown(2);
            builder.addShort(RECOVERY_TIME, UNIT_HOURS);
        }
        builder.addInt(HR_ZONE_EXTREME, UNIT_SECONDS);
        builder.addInt(HR_ZONE_ANAEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_AEROBIC, UNIT_SECONDS);
        builder.addInt(HR_ZONE_FAT_BURN, UNIT_SECONDS);
        builder.addInt(HR_ZONE_WARM_UP, UNIT_SECONDS);
        builder.addUnknown(2);
        builder.addInt(JUMPS, UNIT_JUMPS);
        builder.addShort(JUMP_RATE_AVG, UNIT_JUMPS_PER_MINUTE);
        builder.addUnknown(2);
        builder.addShort(JUMP_RATE_MAX, UNIT_JUMPS_PER_MINUTE);
        if (version == 3) {
            builder.addUnknown(43);   // probably the same as for v5 below
            builder.addUnknown(2);    // configuredJumpsGoal, UNIT_JUMPS, probably includes 2 more bytes
            builder.addUnknown(2);
        } else {
            builder.addUnknown(27);
            builder.addUnknown(4); // activeSeconds again?, UNIT_SECONDS
            builder.addFloat(TRAINING_EFFECT_ANAEROBIC, UNIT_NONE);
            builder.addUnknown(3);
            builder.addInt("configuredTimeGoal", UNIT_SECONDS);
            builder.addShort("configuredCaloriesGoal", UNIT_KCAL);
            builder.addInt("configuredJumpsGoal", UNIT_JUMPS);
            builder.addShort(WORKOUT_LOAD, UNIT_NONE);
            builder.addUnknown(1);
            builder.addByte("vitality_gain", UNIT_NONE); // vitality, UNIT_NONE
        }

        return builder.build();
    }
}



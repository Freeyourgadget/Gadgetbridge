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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CADENCE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CALORIES_BURNT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_AEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_ANAEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_EXTREME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_FAT_BURN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_WARM_UP;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.PACE_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.PACE_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.RECOVERY_TIME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SPEED_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_END;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_START;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_HOURS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KCAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KMPH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NONE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_M;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_UNIX_EPOCH_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.WORKOUT_LOAD;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl.XiaomiSimpleActivityParser.XIAOMI_WORKOUT_TYPE;

import android.widget.Toast;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class WorkoutSummaryParser extends XiaomiActivityParser implements ActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutSummaryParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        BaseActivitySummary summary = new BaseActivitySummary();

        summary.setStartTime(fileId.getTimestamp()); // due to a bug this has to be set
        summary.setEndTime(fileId.getTimestamp()); // due to a bug this has to be set
        summary.setActivityKind(ActivityKind.TYPE_UNKNOWN);
        summary.setRawSummaryData(ArrayUtils.addAll(fileId.toBytes(), bytes));

        try {
            summary = parseBinaryData(summary);
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
    public BaseActivitySummary parseBinaryData(final BaseActivitySummary summary) {
        final ByteBuffer buf = ByteBuffer.wrap(summary.getRawSummaryData()).order(ByteOrder.LITTLE_ENDIAN);

        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(buf);

        XiaomiSimpleActivityParser parser = null;

        switch (fileId.getSubtype()) {
            case SPORTS_OUTDOOR_WALKING_V1:
                summary.setActivityKind(ActivityKind.TYPE_WALKING);
                parser = getOutdoorWalkingV1Parser(fileId);
                break;
            case SPORTS_OUTDOOR_RUNNING:
                summary.setActivityKind(ActivityKind.TYPE_RUNNING);
                // TODO
                break;
            case SPORTS_INDOOR_CYCLING:
                parser = getIndoorCyclingParser(fileId);
                break;
            case SPORTS_FREESTYLE:
                summary.setActivityKind(ActivityKind.TYPE_STRENGTH_TRAINING);
                // TODO
                break;
            case SPORTS_ELLIPTICAL:
                summary.setActivityKind(ActivityKind.TYPE_ELLIPTICAL_TRAINER);
                // TODO
                break;
            case SPORTS_OUTDOOR_WALKING_V2:
                parser = getOutdoorWalkingV2Parser(fileId);
                break;
            case SPORTS_OUTDOOR_CYCLING:
                parser = getOutdoorCyclingParser(fileId);
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
    private XiaomiSimpleActivityParser getIndoorCyclingParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 8:
                headerSize = 7;
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
            case 4:
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
        builder.addUnknown(12);
        builder.addInt(STEPS, UNIT_STEPS);
        builder.addUnknown(2);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);
        builder.addUnknown(20);
        builder.addFloat("recoveryValue", "?");
        builder.addUnknown(9);
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
        builder.addUnknown(24);
        builder.addByte("averageHR2", UNIT_BPM);
        builder.addByte("maxHR2", UNIT_BPM);
        builder.addByte("minHR2", UNIT_BPM);
        builder.addUnknown(2);
        builder.addByte(CADENCE_AVG, UNIT_SPM);

        return builder.build();
    }

    @Nullable
    private XiaomiSimpleActivityParser getOutdoorCyclingParser(final XiaomiActivityFileId fileId) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 4:
                headerSize = 6;
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
        builder.addFloat(SPEED_MAX, UNIT_KMPH);
        builder.addByte(HR_AVG, UNIT_BPM);
        builder.addByte(HR_MAX, UNIT_BPM);
        builder.addByte(HR_MIN, UNIT_BPM);

        return builder.build();
    }
}

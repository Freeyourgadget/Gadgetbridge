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

import android.widget.Toast;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;
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
        builder.addInt("startTime", "seconds");
        builder.addInt("endTime", "seconds");
        builder.addInt("activeSeconds", "seconds");
        builder.addInt("distanceMeters", "meters");
        builder.addShort("caloriesBurnt", "calories_unit");
        builder.addUnknown(4);
        builder.addByte("averageHR", "bpm");
        builder.addByte("maxHR", "bpm");
        builder.addByte("minHR", "bpm");

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
        builder.addInt("startTime", "seconds");
        builder.addInt("endTime", "seconds");
        builder.addInt("activeSeconds", "seconds");
        builder.addInt("distanceMeters", "meters");
        builder.addInt("caloriesBurnt", "calories_unit");
        builder.addInt("maxPace", "seconds_m");
        builder.addInt("minPace", "seconds_m");
        builder.addUnknown(4);
        builder.addInt("steps", "steps_unit");
        builder.addUnknown(2); // pace?
        builder.addByte("averageHR", "bpm");
        builder.addByte("maxHR", "bpm");
        builder.addByte("minHR", "bpm");
        builder.addUnknown(20);
        builder.addFloat("recoveryValue", "recoveryValue");
        builder.addUnknown(9);
        builder.addByte("recoveryTime", "seconds");
        builder.addUnknown(2);
        builder.addInt("vo2max", "seconds");
        builder.addInt("hrZoneAnaerobic", "seconds");
        builder.addInt("hrZoneAerobic", "seconds");
        builder.addInt("hrZoneFatBurn", "seconds");
        builder.addInt("hrZoneWarmUp", "seconds");
        builder.addInt("configured_time_goal", "seconds");

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
        builder.addShort("xiaomiActivityType", "xiaomiActivityType");
        builder.addInt("startTime", "seconds");
        builder.addInt("endTime", "seconds");
        builder.addInt("activeSeconds", "seconds");
        builder.addUnknown(4);
        builder.addInt("distanceMeters", "meters");
        builder.addUnknown(2);
        builder.addShort("caloriesBurnt", "calories_unit");
        builder.addUnknown(12);
        builder.addInt("steps", "steps_unit");
        builder.addUnknown(2);
        builder.addByte("averageHR", "bpm");
        builder.addByte("maxHR", "bpm");
        builder.addByte("minHR", "bpm");
        builder.addUnknown(20);
        builder.addFloat("recoveryValue", "?");
        builder.addUnknown(9);
        builder.addByte("recoveryTime", "seconds");
        builder.addUnknown(2);
        builder.addInt("hrZoneExtreme", "seconds");
        builder.addInt("hrZoneAnaerobic", "seconds");
        builder.addInt("hrZoneAerobic", "seconds");
        builder.addInt("hrZoneFatBurn", "seconds");
        builder.addInt("hrZoneWarmUp", "seconds");
        builder.addInt("configured_time_goal", "seconds");

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
        builder.addShort("xiaomiWorkoutType", "xiaomiWorkoutType");
        builder.addInt("startTime", "seconds");
        builder.addInt("endTime", "seconds");
        builder.addInt("activeSeconds", "seconds");
        builder.addUnknown(4);
        builder.addInt("distanceMeters", "meters");
        builder.addUnknown(2);
        builder.addShort("caloriesBurnt", "calories_unit");
        builder.addUnknown(8);
        builder.addFloat("maxSpeed", "km_h");
        builder.addByte("averageHR", "bpm");
        builder.addByte("maxHR", "bpm");
        builder.addByte("minHR", "bpm");

        return builder.build();
    }
}

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

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

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
        final JSONObject summaryData = new JSONObject();

        final ByteBuffer buf = ByteBuffer.wrap(summary.getRawSummaryData()).order(ByteOrder.LITTLE_ENDIAN);

        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(buf);

        switch (fileId.getSubtype()) {
            case SPORTS_OUTDOOR_RUNNING:
                break;
            case SPORTS_FREESTYLE:
                break;
            case SPORTS_ELLIPTICAL:
                break;
            case SPORTS_OUTDOOR_WALKING:
                return parseOutdoorWalking(summary, fileId, buf);
            case SPORTS_OUTDOOR_CYCLING:
                return parseOutdoorCycling(summary, fileId, buf);
        }

        LOG.warn("Unable to parse {}", fileId.getSubtype());

        return null;
    }

    private BaseActivitySummary parseOutdoorWalking(final BaseActivitySummary summary, final XiaomiActivityFileId fileId, final ByteBuffer buf) {
        final JSONObject summaryData = new JSONObject();

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

        final byte[] header = new byte[headerSize];
        buf.get(header);

        final short workoutType = buf.getShort();

        switch (workoutType) {
            case 2:
                summary.setActivityKind(ActivityKind.TYPE_WALKING);
                break;
            default:
                summary.setActivityKind(ActivityKind.TYPE_UNKNOWN);
        }

        final int startTime = buf.getInt();
        final int endTime = buf.getInt();

        // We don't set the start time, since we need it to match the fileId for the WorkoutGpsParser
        // to find it. They also seem to match.
        //summary.setStartTime(new Date(startTime * 1000L));
        summary.setEndTime(new Date(endTime * 1000L));

        final int duration = buf.getInt();
        addSummaryData(summaryData, "activeSeconds", duration, "seconds");

        final int unknown1 = buf.getInt();
        final int distance = buf.getInt();
        addSummaryData(summaryData, "distanceMeters", distance, "meters");

        final int unknown2 = buf.getShort();

        final int calories = buf.getShort();
        addSummaryData(summaryData, "caloriesBurnt", calories, "calories_unit");

        final int unknown3 = buf.getInt(); // pace?
        final int unknown4 = buf.getInt(); // pace?
        final int unknown5 = buf.getInt(); // pace?
        final int steps = buf.getInt();
        addSummaryData(summaryData, "steps", steps, "steps_unit");
        final int unknown6 = buf.getShort(); // pace?

        final int averageHR = buf.get() & 0xff;
        final int maxHR = buf.get() & 0xff;
        final int minHR = buf.get() & 0xff;

        addSummaryData(summaryData, "averageHR", averageHR, "bpm");
        addSummaryData(summaryData, "maxHR", maxHR, "bpm");
        addSummaryData(summaryData, "minHR", minHR, "bpm");

        summary.setSummaryData(summaryData.toString());

        return summary;
    }

    private BaseActivitySummary parseOutdoorCycling(final BaseActivitySummary summary, final XiaomiActivityFileId fileId, final ByteBuffer buf) {
        final JSONObject summaryData = new JSONObject();

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

        final byte[] header = new byte[headerSize];
        buf.get(header);

        final short workoutType = buf.getShort();

        switch (workoutType) {
            case 6:
                summary.setActivityKind(ActivityKind.TYPE_CYCLING);
                break;
            default:
                summary.setActivityKind(ActivityKind.TYPE_UNKNOWN);
        }

        final int startTime = buf.getInt();
        final int endTime = buf.getInt();

        // We don't set the start time, since we need it to match the fileId for the WorkoutGpsParser
        // to find it. They also seem to match.
        //summary.setStartTime(new Date(startTime * 1000L));
        summary.setEndTime(new Date(endTime * 1000L));

        final int duration = buf.getInt();
        addSummaryData(summaryData, "activeSeconds", duration, "seconds");

        final int unknown1 = buf.getInt();
        final int distance = buf.getInt();
        addSummaryData(summaryData, "distanceMeters", distance, "meters");

        final int unknown2 = buf.getShort();

        final int calories = buf.getShort();
        addSummaryData(summaryData, "caloriesBurnt", calories, "calories_unit");

        final int unknown3 = buf.getInt();
        final int unknown4 = buf.getInt();
        final float maxSpeed = buf.getFloat();

        final float avgHr = buf.get() & 0xff;
        final float maxHr = buf.get() & 0xff;
        final float minHr = buf.get() & 0xff;
        addSummaryData(summaryData, "averageHR", avgHr, "bpm");
        addSummaryData(summaryData, "maxHR", maxHr, "bpm");
        addSummaryData(summaryData, "minHR", minHr, "bpm");

        summary.setSummaryData(summaryData.toString());

        return summary;
    }

    protected void addSummaryData(final JSONObject summaryData, final String key, final float value, final String unit) {
        if (value > 0) {
            try {
                final JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", unit);
                summaryData.put(key, innerData);
            } catch (final JSONException ignore) {
            }
        }
    }

    protected void addSummaryData(final JSONObject summaryData, final String key, final String value) {
        if (key != null && !key.equals("") && value != null && !value.equals("")) {
            try {
                final JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", "string");
                summaryData.put(key, innerData);
            } catch (final JSONException ignore) {
            }
        }
    }
}

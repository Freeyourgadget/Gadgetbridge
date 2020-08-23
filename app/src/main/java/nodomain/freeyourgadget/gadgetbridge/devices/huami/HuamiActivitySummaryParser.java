/*  Copyright (C) 2020 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSportsActivityType;

public class HuamiActivitySummaryParser implements ActivitySummaryParser {

    private static final Logger LOG = LoggerFactory.getLogger(HuamiActivityDetailsParser.class);
    private JSONObject summaryData = new JSONObject();


    public BaseActivitySummary parseBinaryData(BaseActivitySummary summary) {
        Date startTime = summary.getStartTime();
        if (startTime == null) {
            LOG.error("Due to a bug, we can only parse the summary when startTime is already set");
            return null;
        }
        return parseBinaryData(summary, startTime);
    }

    private BaseActivitySummary parseBinaryData(BaseActivitySummary summary, Date startTime) {
        summaryData = new JSONObject();
        ByteBuffer buffer = ByteBuffer.wrap(summary.getRawSummaryData()).order(ByteOrder.LITTLE_ENDIAN);

        short version = buffer.getShort(); // version
        LOG.debug("Got sport summary version " + version + " total bytes=" + buffer.capacity());
        int activityKind = ActivityKind.TYPE_UNKNOWN;
        try {
            int rawKind = BLETypeConversions.toUnsigned(buffer.getShort());
            HuamiSportsActivityType activityType = HuamiSportsActivityType.fromCode(rawKind);
            activityKind = activityType.toActivityKind();
        } catch (Exception ex) {
            LOG.error("Error mapping activity kind: " + ex.getMessage(), ex);
        }
        summary.setActivityKind(activityKind);

        // FIXME: should honor timezone we were in at that time etc
        long timestamp_start = BLETypeConversions.toUnsigned(buffer.getInt()) * 1000;
        long timestamp_end = BLETypeConversions.toUnsigned(buffer.getInt()) * 1000;


        // FIXME: should be done like this but seems to return crap when in DST
        //summary.setStartTime(new Date(timestamp_start));
        //summary.setEndTime(new Date(timestamp_end));

        // FIXME ... so do it like this
        long duration = timestamp_end - timestamp_start;
        summary.setEndTime(new Date(startTime.getTime() + duration));

        int baseLongitude = buffer.getInt();
        int baseLatitude = buffer.getInt();
        int baseAltitude = buffer.getInt();
        summary.setBaseLongitude(baseLongitude);
        summary.setBaseLatitude(baseLatitude);
        summary.setBaseAltitude(baseAltitude);

        int steps;
        int activeSeconds;
        int maxLatitude;
        int minLatitude;
        int maxLongitude;
        int minLongitude;
        float caloriesBurnt;
        float distanceMeters;
        float ascentMeters = 0;
        float descentMeters = 0;
        float maxAltitude = 0;
        float minAltitude = 0;
        float maxSpeed = 0;
        float minPace;
        float maxPace;
        float totalStride = 0;
        float averageStride;
        short averageHR;
        short maxHR = 0;
        short averageKMPaceSeconds;
        int ascentSeconds = 0;
        int descentSeconds = 0;
        int flatSeconds = 0;

        // Swimming
        float averageStrokeDistance = 0;
        float averageStrokesPerSecond = 0;
        float averageLapPace = 0;
        short strokes = 0;
        short swolfIndex = 0; // this is called SWOLF score on bip s, SWOLF index on mi band 4
        byte swimStyle = 0;
        byte laps = 0;

        // Just assuming, Bip has 259 which seems like 256+x
        // Bip S now has 518 so assuming 512+x, might be wrong

        if (version >= 512) {
            steps = buffer.getInt();
            activeSeconds = buffer.getInt();
            //unknown
            buffer.getLong();
            buffer.getLong();
            caloriesBurnt = buffer.getFloat();
            distanceMeters = buffer.getFloat();
            ascentMeters = buffer.getFloat();
            descentMeters = buffer.getFloat();
            maxAltitude = buffer.getFloat();
            minAltitude = buffer.getFloat();
            //unknown
            buffer.getLong();
            buffer.getLong();
            minPace = buffer.getFloat();
            maxPace = buffer.getFloat();
            //unknown
            buffer.getLong();
            buffer.getLong();
            buffer.getLong();
            buffer.getLong();
            buffer.getInt();
            averageHR = buffer.getShort();
            averageKMPaceSeconds = buffer.getShort();
            averageStride = buffer.getShort();
            maxHR = buffer.getShort();

            if (activityKind == ActivityKind.TYPE_CYCLING || activityKind == ActivityKind.TYPE_RUNNING) {
                // this had nonsense data with treadmill on bip s, need to test it with running
                // for cycling it seems to work... hmm...
                // 28 bytes
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                ascentSeconds = buffer.getInt() / 1000; //ms?
                buffer.getInt(); // unknown;
                descentSeconds = buffer.getInt() / 1000; //ms?
                buffer.getInt(); // unknown;
                flatSeconds = buffer.getInt() / 1000; // ms?
            } else if (activityKind == ActivityKind.TYPE_SWIMMING || activityKind == ActivityKind.TYPE_SWIMMING_OPENWATER) {
                // offset 0x8c
                /*
                    data on the bip s display (example)
                    main style backstroke
                    SWOLF score 92
                    total laps 1
                    avg. pace 2,09/100
                    strokes 36
                    avg stroke rate 26 spm
                    single stroke distance 1,79m
                    max stroke rate 39
                 */

                averageStrokeDistance = buffer.getFloat();
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                averageStrokesPerSecond = buffer.getFloat();
                averageLapPace = buffer.getFloat();
                buffer.getInt(); // unknown
                strokes = buffer.getShort();
                swolfIndex = buffer.getShort();
                swimStyle = buffer.get();
                laps = buffer.get();
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
            }
        } else {
            distanceMeters = buffer.getFloat();
            ascentMeters = buffer.getFloat();
            descentMeters = buffer.getFloat();
            minAltitude = buffer.getFloat();
            maxAltitude = buffer.getFloat();
            maxLatitude = buffer.getInt(); // format?
            minLatitude = buffer.getInt(); // format?
            maxLongitude = buffer.getInt(); // format?
            minLongitude = buffer.getInt(); // format?
            steps = buffer.getInt();
            activeSeconds = buffer.getInt();
            caloriesBurnt = buffer.getFloat();
            maxSpeed = buffer.getFloat();
            maxPace = buffer.getFloat();
            minPace = buffer.getFloat();
            totalStride = buffer.getFloat();

            buffer.getInt(); // unknown
            if (activityKind == ActivityKind.TYPE_SWIMMING) {
                // 28 bytes
                averageStrokeDistance = buffer.getFloat();
                averageStrokesPerSecond = buffer.getFloat();
                averageLapPace = buffer.getFloat();
                strokes = buffer.getShort();
                swolfIndex = buffer.getShort();
                swimStyle = buffer.get();
                laps = buffer.get();
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                buffer.getShort(); // unknown
            } else {
                // 28 bytes
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                ascentSeconds = buffer.getInt() / 1000; //ms?
                buffer.getInt(); // unknown;
                descentSeconds = buffer.getInt() / 1000; //ms?
                buffer.getInt(); // unknown;
                flatSeconds = buffer.getInt() / 1000; // ms?

                addSummaryData("ascentSeconds", ascentSeconds, "seconds");
                addSummaryData("descentSeconds", descentSeconds, "seconds");
                addSummaryData("flatSeconds", flatSeconds, "seconds");
            }

            averageHR = buffer.getShort();

            averageKMPaceSeconds = buffer.getShort();
            averageStride = buffer.getShort();
        }

//        summary.setBaseCoordinate(new GPSCoordinate(baseLatitude, baseLongitude, baseAltitude));
//        summary.setDistanceMeters(distanceMeters);
//        summary.setAscentMeters(ascentMeters);
//        summary.setDescentMeters(descentMeters);
//        summary.setMinAltitude(maxAltitude);
//        summary.setMaxAltitude(maxAltitude);
//        summary.setMinLatitude(minLatitude);
//        summary.setMaxLatitude(maxLatitude);
//        summary.setMinLongitude(minLatitude);
//        summary.setMaxLongitude(maxLatitude);
//        summary.setSteps(steps);
//        summary.setActiveTimeSeconds(secondsActive);
//        summary.setCaloriesBurnt(caloriesBurnt);
//        summary.setMaxSpeed(maxSpeed);
//        summary.setMinPace(minPace);
//        summary.setMaxPace(maxPace);
//        summary.setTotalStride(totalStride);
//        summary.setTimeAscent(BLETypeConversions.toUnsigned(ascentSeconds);
//        summary.setTimeDescent(BLETypeConversions.toUnsigned(descentSeconds);
//        summary.setTimeFlat(BLETypeConversions.toUnsigned(flatSeconds);
//        summary.setAverageHR(BLETypeConversions.toUnsigned(averageHR);
//        summary.setAveragePace(BLETypeConversions.toUnsigned(averagePace);
//        summary.setAverageStride(BLETypeConversions.toUnsigned(averageStride);

        addSummaryData("ascentSeconds", ascentSeconds, "seconds");
        addSummaryData("descentSeconds", descentSeconds, "seconds");
        addSummaryData("flatSeconds", flatSeconds, "seconds");

        addSummaryData("distanceMeters", distanceMeters, "meters");
        addSummaryData("ascentMeters", ascentMeters, "meters");
        addSummaryData("descentMeters", descentMeters, "meters");
        if (maxAltitude != -100000) {
            addSummaryData("maxAltitude", maxAltitude, "meters");
        }
        if (minAltitude != 100000) {
            addSummaryData("minAltitude", minAltitude, "meters");
        }
        addSummaryData("steps", steps, "steps_unit");
        addSummaryData("activeSeconds", activeSeconds, "seconds");
        addSummaryData("caloriesBurnt", caloriesBurnt, "calories_unit");
        addSummaryData("maxSpeed", maxSpeed, "meters_second");

        if (!(activityKind == ActivityKind.TYPE_ELLIPTICAL_TRAINER ||
                activityKind == ActivityKind.TYPE_JUMP_ROPING ||
                activityKind == ActivityKind.TYPE_EXERCISE ||
                activityKind == ActivityKind.TYPE_YOGA ||
                activityKind == ActivityKind.TYPE_INDOOR_CYCLING)) {
            addSummaryData("minPace", minPace, "seconds_m");
            addSummaryData("maxPace", maxPace, "seconds_m");
        }

        addSummaryData("totalStride", totalStride, "meters");
        addSummaryData("averageHR", averageHR, "bpm");
        addSummaryData("maxHR", maxHR, "bpm");
        addSummaryData("averageKMPaceSeconds", averageKMPaceSeconds, "seconds_km");
        addSummaryData("averageStride", averageStride, "cm");

        if (activityKind == ActivityKind.TYPE_SWIMMING || activityKind == ActivityKind.TYPE_SWIMMING_OPENWATER) {
            addSummaryData("averageStrokeDistance", averageStrokeDistance, "meters");
            addSummaryData("averageStrokesPerSecond", averageStrokesPerSecond, "strokes_second");
            addSummaryData("averageLapPace", averageLapPace, "second");
            addSummaryData("strokes", strokes, "strokes");
            addSummaryData("swolfIndex", swolfIndex, "swolf_index");
            String swimStyleName = "unknown"; // TODO: translate here or keep as string identifier here?
            switch (swimStyle) {
                case 1:
                    swimStyleName = "breaststroke";
                    break;
                case 2:
                    swimStyleName = "freestyle";
                    break;
                case 3:
                    swimStyleName = "backstroke";
                    break;
                case 4:
                    swimStyleName = "medley";
                    break;
            }
            addSummaryData("swimStyle", swimStyleName);
            addSummaryData("laps", laps, "laps");
        }

        summary.setSummaryData(summaryData.toString());
        return summary;
    }


    private void addSummaryData(String key, float value, String unit) {
        if (value > 0) {
            try {
                JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", unit);
                summaryData.put(key, innerData);
            } catch (JSONException ignore) {
            }
        }
    }

    private void addSummaryData(String key, String value) {
        if (key != null && !key.equals("") && value != null && !value.equals("")) {
            try {
                JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", "string");
                summaryData.put(key, innerData);
            } catch (JSONException ignore) {
            }
        }
    }
}

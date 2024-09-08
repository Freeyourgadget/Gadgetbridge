/*  Copyright (C) 2020-2024 José Rebelo, Petr Vaněk, Reiner Herrmann,
    Sebastian Krey

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

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;

public class ActivitySummaryJsonSummary {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryJsonSummary.class);
    private JSONObject groupData;
    private JSONObject summaryData;
    private JSONObject summaryGroupedList;
    private ActivitySummaryParser summaryParser;
    private BaseActivitySummary baseActivitySummary;

    public ActivitySummaryJsonSummary(final ActivitySummaryParser summaryParser, BaseActivitySummary baseActivitySummary){
        this.summaryParser=summaryParser;
        this.baseActivitySummary=baseActivitySummary;
    }

    private JSONObject setSummaryData(BaseActivitySummary item, final boolean forDetails){
        String summary = getCorrectSummary(item, forDetails);
        JSONObject jsonSummary = getJSONSummary(summary);
        if (jsonSummary != null) {
            //add additionally computed values here

            if (item.getBaseAltitude() != null && item.getBaseAltitude() != -20000) {
                JSONObject baseAltitudeValues;
                try {
                    baseAltitudeValues = new JSONObject();
                    baseAltitudeValues.put("value", item.getBaseAltitude());
                    baseAltitudeValues.put("unit", "meters");
                    jsonSummary.put("baseAltitude", baseAltitudeValues);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (jsonSummary.has("distanceMeters") && jsonSummary.has("activeSeconds")) {
                JSONObject averageSpeed;
                try {
                    JSONObject distanceMeters = (JSONObject) jsonSummary.get("distanceMeters");
                    JSONObject activeSeconds = (JSONObject) jsonSummary.get("activeSeconds");
                    double distance = distanceMeters.getDouble("value");
                    double duration = activeSeconds.getDouble("value");
                    averageSpeed = new JSONObject();
                    averageSpeed.put("value", distance / duration);
                    averageSpeed.put("unit", "meters_second");
                    jsonSummary.put("averageSpeed", averageSpeed);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonSummary;
    }

    public JSONObject getSummaryData(final boolean forDetails){
        //returns json with summaryData
        if (summaryData==null) summaryData=setSummaryData(baseActivitySummary, forDetails);
        return summaryData;
    }

    private String getCorrectSummary(BaseActivitySummary item, final boolean forDetails){
        try {
            item = summaryParser.parseBinaryData(item, forDetails);
        } catch (final Exception e) {
            LOG.error("Failed to re-parse corrected summary", e);
        }
        return item.getSummaryData();
    }

    private JSONObject getJSONSummary(String sumData){
        JSONObject summarySubdata = null;
        if (sumData != null) {
            try {
                summarySubdata = new JSONObject(sumData);
            } catch (JSONException e) {
            }
        }
        return summarySubdata;
    }

    public JSONObject getSummaryGroupedList() {
        //returns list grouped by activity groups as per createActivitySummaryGroups
        if (summaryData==null) summaryData=setSummaryData(baseActivitySummary, true);
        if (summaryGroupedList==null) summaryGroupedList=setSummaryGroupedList(summaryData);
        return summaryGroupedList;
    }
    private JSONObject setSummaryGroupedList(JSONObject summaryDatalist){
        this.groupData = createActivitySummaryGroups(); //structure for grouping activities into groups, when vizualizing

        if (summaryDatalist == null) return null;
        Iterator<String> keys = summaryDatalist.keys();

        final Map<String, JSONArray> activeGroups = new LinkedHashMap<>();
        // Initialize activeGroups with the initial expected order and empty arrays
        final Iterator<String> names = this.groupData.keys();
        while (names.hasNext()) {
            activeGroups.put(names.next(), new JSONArray());
        }

        while (keys.hasNext()) {
            String key = keys.next();
            if (INTERNAL_HAS_GPS.equals(key)) {
                continue;
            }
            try {
                JSONObject innerData = (JSONObject) summaryDatalist.get(key);
                Object value = innerData.get("value");
                String unit = innerData.getString("unit");
                // Use the group if specified in the entry, otherwise fallback to the array below
                String groupName = innerData.optString("group", getGroup(key));

                JSONArray group = activeGroups.get(groupName);
                if (group == null) {
                    // This group is not defined in createActivitySummaryGroups - add it to the end
                    group = new JSONArray();
                    activeGroups.put(groupName, group);
                }

                JSONObject item = new JSONObject();
                item.put("name", key);
                item.put("value", value);
                item.put("unit", unit);
                group.put(item);
            } catch (JSONException e) {
                LOG.error("SportsActivity internal error building grouped summary", e);
            }
        }

        // Convert activeGroups to the expected JSONObject
        // activeGroups is already ordered
        final JSONObject grouped = new JSONObject();
        for (final Map.Entry<String, JSONArray> entry : activeGroups.entrySet()) {
            if (entry.getValue().length() == 0) {
                // empty group
                continue;
            }
            try {
                grouped.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                LOG.error("SportsActivity internal error building grouped summary", e);
            }
        }
        return grouped;
    }

    private String getGroup(String searchItem) {
        // NB: Default group must be present in group JSONObject created by createActivitySummaryGroups
        String defaultGroup = "Activity";
        if (groupData == null) return defaultGroup;
        Iterator<String> keys = groupData.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                JSONArray itemList = (JSONArray) groupData.get(key);
                for (int i = 0; i < itemList.length(); i++) {
                    if (itemList.getString(i).equals(searchItem)) {
                        return key;
                    }
                }
            } catch (JSONException e) {
                LOG.error("SportsActivity", e);
            }
        }
        return defaultGroup;
    }

    /** @noinspection ArraysAsListWithZeroOrOneArgument*/
    private JSONObject createActivitySummaryGroups(){
        final Map<String, List<String>> groupDefinitions = new LinkedHashMap<String, List<String>>() {{
            // NB: Default group Activity must be present in this definition, otherwise it wouldn't
            // be shown.
            put("Activity", Arrays.asList(
                    DISTANCE_METERS, STEPS, STEP_RATE_SUM, ACTIVE_SECONDS, CALORIES_BURNT,
                    STRIDE_TOTAL, HR_AVG, HR_MAX, HR_MIN, STRIDE_AVG, STRIDE_MAX, STRIDE_MIN,
                    STEP_LENGTH_AVG
            ));
            put("Speed", Arrays.asList(
                    SPEED_AVG, SPEED_MAX, SPEED_MIN, PACE_AVG_SECONDS_KM, PACE_MIN,
                    PACE_MAX, "averageSpeed2", CADENCE_AVG, CADENCE_MAX, CADENCE_MIN,
                    STEP_RATE_AVG
            ));
            put("Elevation", Arrays.asList(
                    ASCENT_METERS, DESCENT_METERS, ALTITUDE_MAX, ALTITUDE_MIN, ALTITUDE_AVG,
                    ALTITUDE_BASE, ASCENT_SECONDS, DESCENT_SECONDS, FLAT_SECONDS, ASCENT_DISTANCE,
                    DESCENT_DISTANCE, FLAT_DISTANCE, ELEVATION_GAIN, ELEVATION_LOSS
            ));
            put("HeartRateZones", Arrays.asList(
                    HR_ZONE_NA, HR_ZONE_WARM_UP, HR_ZONE_FAT_BURN, HR_ZONE_AEROBIC, HR_ZONE_ANAEROBIC,
                    HR_ZONE_EXTREME
            ));
            put("Strokes", Arrays.asList(
                    STROKE_DISTANCE_AVG, STROKE_AVG_PER_SECOND, STROKES,
                    STROKE_RATE_AVG, STROKE_RATE_MAX
            ));
            put("Swimming", Arrays.asList(
                    SWOLF_INDEX, SWOLF_AVG, SWOLF_MAX, SWOLF_MIN, SWIM_STYLE
            ));
            put("TrainingEffect", Arrays.asList(
                    TRAINING_EFFECT_AEROBIC, TRAINING_EFFECT_ANAEROBIC, WORKOUT_LOAD,
                    MAXIMUM_OXYGEN_UPTAKE, RECOVERY_TIME, LACTATE_THRESHOLD_HR
            ));
            put("laps", Arrays.asList(
                    LAP_PACE_AVERAGE, LAPS, LANE_LENGTH
            ));
            put("Pace", Arrays.asList(
            ));
            put("RunningForm", Arrays.asList(
                    GROUND_CONTACT_TIME_AVG, IMPACT_AVG, IMPACT_MAX, SWING_ANGLE_AVG,
                    FORE_FOOT_LANDINGS, MID_FOOT_LANDINGS, BACK_FOOT_LANDINGS,
                    EVERSION_ANGLE_AVG, EVERSION_ANGLE_MAX
            ));
            put(SETS, Arrays.asList(
            ));
        }};

        return new JSONObject(groupDefinitions);
    }
}

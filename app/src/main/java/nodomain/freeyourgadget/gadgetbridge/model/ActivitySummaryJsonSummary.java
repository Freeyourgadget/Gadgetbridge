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

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;

public class ActivitySummaryJsonSummary {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryJsonSummary.class);
    private Map<String, List<String>> groupData;
    private ActivitySummaryData summaryData;
    private Map<String, List<Pair<String, ActivitySummaryEntry>>> summaryGroupedList;
    private final ActivitySummaryParser summaryParser;
    private final BaseActivitySummary baseActivitySummary;

    public ActivitySummaryJsonSummary(final ActivitySummaryParser summaryParser, BaseActivitySummary baseActivitySummary) {
        this.summaryParser = summaryParser;
        this.baseActivitySummary = baseActivitySummary;
    }

    private ActivitySummaryData setSummaryData(BaseActivitySummary item, final boolean forDetails) {
        final ActivitySummaryData summary = ActivitySummaryData.fromJson(getCorrectSummary(item, forDetails));
        if (summary == null) {
            return null;
        }

        //add additionally computed values here
        if (item.getBaseAltitude() != null && item.getBaseAltitude() != -20000 && !summary.has("baseAltitude")) {
            summary.add("baseAltitude", item.getBaseAltitude(), UNIT_METERS);
        }

        if (!summary.has("averageSpeed") && summary.has("distanceMeters") && summary.has("activeSeconds")) {
            double distance = summary.getNumber("distanceMeters", 0).doubleValue();
            double duration = summary.getNumber("activeSeconds", 1).doubleValue();
            summary.add("averageSpeed", distance / duration, UNIT_METERS_PER_SECOND);
        }

        if (!summary.has(STEP_RATE_AVG) && summary.has(STEPS) && summary.has(ACTIVE_SECONDS)) {
            double stepcount = summary.getNumber(STEPS, 0).doubleValue();
            double duration = summary.getNumber(ACTIVE_SECONDS, 1).doubleValue();
            summary.add(STEP_RATE_AVG, (double)((int)(((stepcount / duration) * 60)+0.5)), UNIT_SPM);
        }

        return summary;
    }

    public ActivitySummaryData getSummaryData(final boolean forDetails) {
        //returns json with summaryData
        if (summaryData == null) summaryData = setSummaryData(baseActivitySummary, forDetails);
        return summaryData;
    }

    private String getCorrectSummary(BaseActivitySummary item, final boolean forDetails) {
        if (summaryParser == null) {
            return item.getSummaryData();
        }
        try {
            item = summaryParser.parseBinaryData(item, forDetails);
        } catch (final Exception e) {
            LOG.error("Failed to re-parse corrected summary", e);
        }
        return item.getSummaryData();
    }

    public Map<String, List<Pair<String, ActivitySummaryEntry>>> getSummaryGroupedList() {
        //returns list grouped by activity groups as per createActivitySummaryGroups
        if (summaryData == null) summaryData = setSummaryData(baseActivitySummary, true);
        if (summaryGroupedList == null) summaryGroupedList = setSummaryGroupedList(summaryData);
        return summaryGroupedList;
    }

    private Map<String, List<Pair<String, ActivitySummaryEntry>>> setSummaryGroupedList(ActivitySummaryData activitySummaryData) {
        this.groupData = createActivitySummaryGroups(); //structure for grouping activities into groups, when visualizing

        if (activitySummaryData == null) return null;
        Iterator<String> keys = activitySummaryData.getKeys().iterator();

        final Map<String, List<Pair<String, ActivitySummaryEntry>>> activeGroups = new LinkedHashMap<>();
        // Initialize activeGroups with the initial expected order and empty arrays
        for (final String key : this.groupData.keySet()) {
            activeGroups.put(key, new LinkedList<>());
        }

        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith("internal")) {
                continue;
            }
            ActivitySummaryEntry item = activitySummaryData.get(key);
            // Use the group if specified in the entry, otherwise fallback to the array below
            String groupName = item.getGroup() != null ? item.getGroup() : getDefaultGroup(key);

            List<Pair<String, ActivitySummaryEntry>> group = activeGroups.get(groupName);
            if (group == null) {
                // This group is not defined in createActivitySummaryGroups - add it to the end
                group = new LinkedList<>();
                activeGroups.put(groupName, group);
            }

            group.add(Pair.of(key, item));
        }

        // Convert activeGroups to the expected JSONObject
        // activeGroups is already ordered
        final Map<String, List<Pair<String, ActivitySummaryEntry>>> grouped = new LinkedHashMap<>();
        for (final Map.Entry<String, List<Pair<String, ActivitySummaryEntry>>> entry : activeGroups.entrySet()) {
            if (entry.getValue().isEmpty()) {
                // empty group
                continue;
            }
            grouped.put(entry.getKey(), entry.getValue());
        }
        return grouped;
    }

    private String getDefaultGroup(final String searchItem) {
        final String defaultGroup = GROUP_ACTIVITY;
        if (groupData == null) return defaultGroup;
        for (final String groupKey : groupData.keySet()) {
            final List<String> itemList = groupData.get(groupKey);
            if (itemList == null) {
                continue;
            }
            for (final String itemKey : itemList) {
                if (itemKey.equals(searchItem)) {
                    return groupKey;
                }
            }
        }

        // NB: Default group must be present in group JSONObject created by createActivitySummaryGroups
        return defaultGroup;
    }

    /**
     * @noinspection ArraysAsListWithZeroOrOneArgument
     */
    private static Map<String, List<String>> createActivitySummaryGroups() {
        return new LinkedHashMap<String, List<String>>() {{
            // NB: Default group Activity must be present in this definition, otherwise it wouldn't
            // be shown.
            put(GROUP_ACTIVITY, Arrays.asList(
                    DISTANCE_METERS, STEPS, STEP_RATE_SUM, ACTIVE_SECONDS, CALORIES_BURNT,
                    STRIDE_TOTAL, HR_AVG, HR_MAX, HR_MIN, STRIDE_AVG, STRIDE_MAX, STRIDE_MIN,
                    STEP_LENGTH_AVG
            ));
            put(GROUP_SPEED, Arrays.asList(
                    SPEED_AVG, SPEED_MAX, SPEED_MIN, PACE_AVG_SECONDS_KM, PACE_MIN,
                    PACE_MAX, "averageSpeed2", CADENCE_AVG, CADENCE_MAX, CADENCE_MIN,
                    STEP_RATE_AVG, STEP_RATE_MAX
            ));
            put(GROUP_ELEVATION, Arrays.asList(
                    ASCENT_METERS, DESCENT_METERS, ALTITUDE_MAX, ALTITUDE_MIN, ALTITUDE_AVG,
                    ALTITUDE_BASE, ASCENT_SECONDS, DESCENT_SECONDS, FLAT_SECONDS, ASCENT_DISTANCE,
                    DESCENT_DISTANCE, FLAT_DISTANCE, ELEVATION_GAIN, ELEVATION_LOSS
            ));
            put(GROUP_STROKES, Arrays.asList(
                    STROKE_DISTANCE_AVG, STROKE_AVG_PER_SECOND, STROKES,
                    STROKE_RATE_AVG, STROKE_RATE_MAX
            ));
            put(GROUP_JUMPS, Arrays.asList(
                    JUMPS, JUMP_RATE_AVG, JUMP_RATE_MAX
            ));
            put(GROUP_SWIMMING, Arrays.asList(
                    SWOLF_INDEX, SWOLF_AVG, SWOLF_MAX, SWOLF_MIN, SWIM_STYLE
            ));
            put(GROUP_TRAINING_EFFECT, Arrays.asList(
                    TRAINING_EFFECT_AEROBIC, TRAINING_EFFECT_ANAEROBIC, WORKOUT_LOAD,
                    MAXIMUM_OXYGEN_UPTAKE, RECOVERY_TIME, LACTATE_THRESHOLD_HR
            ));
            put(GROUP_LAPS, Arrays.asList(
                    LAP_PACE_AVERAGE, LAPS, LANE_LENGTH
            ));
            put(GROUP_PACE, Arrays.asList(
            ));
            put(GROUP_RUNNING_FORM, Arrays.asList(
                    GROUND_CONTACT_TIME_AVG, IMPACT_AVG, IMPACT_MAX, SWING_ANGLE_AVG,
                    FORE_FOOT_LANDINGS, MID_FOOT_LANDINGS, BACK_FOOT_LANDINGS,
                    EVERSION_ANGLE_AVG, EVERSION_ANGLE_MAX
            ));
            put(GROUP_HEART_RATE_ZONES, Arrays.asList(
                    HR_ZONE_NA, HR_ZONE_WARM_UP, HR_ZONE_FAT_BURN, HR_ZONE_EASY, HR_ZONE_AEROBIC, HR_ZONE_ANAEROBIC,
                    HR_ZONE_THRESHOLD, HR_ZONE_EXTREME, HR_ZONE_MAXIMUM
            ));
            put(SETS, Arrays.asList(
            ));
        }};
    }
}

package nodomain.freeyourgadget.gadgetbridge.model;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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

    private JSONObject setSummaryData(BaseActivitySummary item){
        String summary = getCorrectSummary(item);
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

    public JSONObject getSummaryData(){
        //returns json with summaryData
        if (summaryData==null) summaryData=setSummaryData(baseActivitySummary);
        return summaryData;
    }

    private String getCorrectSummary(BaseActivitySummary item){
        if (item.getRawSummaryData() != null) {
            try {
                item = summaryParser.parseBinaryData(item);
            } catch (final Exception e) {
                LOG.error("Failed to re-parse corrected summary", e);
            }
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
        if (summaryData==null) summaryData=setSummaryData(baseActivitySummary);
        if (summaryGroupedList==null) summaryGroupedList=setSummaryGroupedList(summaryData);
        return summaryGroupedList;
    }
    private JSONObject setSummaryGroupedList(JSONObject summaryDatalist){
        this.groupData = createActivitySummaryGroups(); //structure for grouping activities into groups, when vizualizing

        if (summaryDatalist ==null ) return null;
        Iterator<String> keys = summaryDatalist.keys();
        JSONObject list=new JSONObject();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                JSONObject innerData = (JSONObject) summaryDatalist.get(key);
                Object value = innerData.get("value");
                String unit = innerData.getString("unit");
                String group = getGroup(key);

                if (!list.has(group)) {
                    list.put(group,new JSONArray());
                }

                JSONArray tmpl = (JSONArray) list.get(group);
                JSONObject innernew = new JSONObject();
                innernew.put("name", key);
                innernew.put("value", value);
                innernew.put("unit", unit);
                tmpl.put(innernew);
                list.put(group, tmpl);
            } catch (JSONException e) {
                LOG.error("SportsActivity", e);
            }
        }
        return list;
    }

    private String getGroup(String searchItem) {
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
    private JSONObject createActivitySummaryGroups(){
        final Map<String, List<String>> groupDefinitions = new HashMap<String, List<String>>() {{
            put("Strokes", Arrays.asList(
                    STROKE_DISTANCE_AVG, STROKE_AVG_PER_SECOND, STROKES,
                    STROKE_RATE_AVG, STROKE_RATE_MAX
            ));
            put("Swimming", Arrays.asList(
                    SWOLF_INDEX, SWIM_STYLE
            ));
            put("Elevation", Arrays.asList(
                    ASCENT_METERS, DESCENT_METERS, ALTITUDE_MAX, ALTITUDE_MIN, ALTITUDE_AVG,
                    ALTITUDE_BASE, ASCENT_SECONDS, DESCENT_SECONDS, FLAT_SECONDS, ASCENT_DISTANCE,
                    DESCENT_DISTANCE, FLAT_DISTANCE, ELEVATION_GAIN, ELEVATION_LOSS
            ));
            put("Speed", Arrays.asList(
                    SPEED_AVG, SPEED_MAX, SPEED_MIN, PACE_AVG_SECONDS_KM, PACE_MIN,
                    PACE_MAX, "averageSpeed2", CADENCE_AVG, CADENCE_MAX, CADENCE_MIN
            ));
            put("Activity", Arrays.asList(
                    DISTANCE_METERS, STEPS, ACTIVE_SECONDS, CALORIES_BURNT, STRIDE_TOTAL,
                    HR_AVG, HR_MAX, HR_MIN, STRIDE_AVG, STRIDE_MAX, STRIDE_MIN
            ));
            put("HeartRateZones", Arrays.asList(
                    HR_ZONE_NA, HR_ZONE_WARM_UP, HR_ZONE_FAT_BURN, HR_ZONE_AEROBIC, HR_ZONE_ANAEROBIC,
                    HR_ZONE_EXTREME
            ));
            put("TrainingEffect", Arrays.asList(
                    TRAINING_EFFECT_AEROBIC, TRAINING_EFFECT_ANAEROBIC, WORKOUT_LOAD,
                    MAXIMUM_OXYGEN_UPTAKE
            ));
            put("laps", Arrays.asList(
                    LAP_PACE_AVERAGE, LAPS, LANE_LENGTH
            ));
        }};

        return new JSONObject(groupDefinitions);
    }
}

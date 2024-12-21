package nodomain.freeyourgadget.gadgetbridge.model;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryProgressEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummarySimpleEntry;

public class ActivitySummaryDataTest {
    /**
     * Ensure that we can still deserialize old workouts that consisted of manual json and had
     * no explicit type.
     */
    @Test
    public void deserializeOld() {
        final String json = "{\n" +
                "    \"activeSeconds\": {\n" +
                "        \"group\": \"Some Group\",\n" +
                "        \"value\": 3828,\n" +
                "        \"unit\": \"seconds\"\n" +
                "    },\n" +
                "    \"averageHR\": {\n" +
                "        \"value\": 81,\n" +
                "        \"unit\": \"bpm\"\n" +
                "    },\n" +
                "    \"maxHR\": {\n" +
                "        \"value\": 115,\n" +
                "        \"unit\": \"bpm\"\n" +
                "    },\n" +
                "    \"minHR\": {\n" +
                "        \"value\": 61,\n" +
                "        \"unit\": \"bpm\"\n" +
                "    },\n" +
                "    \"caloriesBurnt\": {\n" +
                "        \"value\": 228,\n" +
                "        \"unit\": \"calories_unit\"\n" +
                "    },\n" +
                "    \"hrZoneNa\": {\n" +
                "        \"value\": 3365,\n" +
                "        \"unit\": \"seconds\"\n" +
                "    },\n" +
                "    \"hrZoneWarmUp\": {\n" +
                "        \"value\": 447,\n" +
                "        \"unit\": \"seconds\"\n" +
                "    },\n" +
                "    \"aerobicTrainingEffect\": {\n" +
                "        \"value\": 0.20000000298023224,\n" +
                "        \"unit\": \"\"\n" +
                "    },\n" +
                "    \"currentWorkoutLoad\": {\n" +
                "        \"value\": 1,\n" +
                "        \"unit\": \"\"\n" +
                "    }\n" +
                "}";

        final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(json);

        assertNotNull(summaryData);

        final Map<String, ActivitySummaryEntry> expected = new LinkedHashMap<String, ActivitySummaryEntry>() {{
            put("activeSeconds", new ActivitySummarySimpleEntry("Some Group", 3828, "seconds"));
            put("averageHR", new ActivitySummarySimpleEntry(81, "bpm"));
            put("maxHR", new ActivitySummarySimpleEntry(115, "bpm"));
            put("minHR", new ActivitySummarySimpleEntry(61, "bpm"));
            put("caloriesBurnt", new ActivitySummarySimpleEntry(228, "calories_unit"));
            put("hrZoneNa", new ActivitySummarySimpleEntry(3365, "seconds"));
            put("hrZoneWarmUp", new ActivitySummarySimpleEntry(447, "seconds"));
            put("aerobicTrainingEffect", new ActivitySummarySimpleEntry(0.20000000298023224, ""));
            put("currentWorkoutLoad", new ActivitySummarySimpleEntry(1, ""));
        }};

        final List<String> keys = new ArrayList<>(summaryData.getKeys());
        assertEquals(new ArrayList<>(expected.keySet()), keys);

        for (final Map.Entry<String, ActivitySummaryEntry> e : expected.entrySet()) {
            final ActivitySummaryEntry jsonEntry = summaryData.get(e.getKey());
            assertTrue(jsonEntry instanceof ActivitySummarySimpleEntry);
            assertEquals(e.getValue().getGroup(), jsonEntry.getGroup());

            Number expectedValue = (Number) ((ActivitySummarySimpleEntry) e.getValue()).getValue();
            Number actualValue = (Number) ((ActivitySummarySimpleEntry) jsonEntry).getValue();

            assertEquals(expectedValue.doubleValue(), actualValue.doubleValue(), 0.000000001d);
            assertEquals(((ActivitySummarySimpleEntry) e.getValue()).getUnit(), ((ActivitySummarySimpleEntry) jsonEntry).getUnit());
        }
    }

    @Test
    public void deserializeSerializeNew() {
        final String json = "{" +
                "\"test_progress\":{" +
                "\"type\":\"progress\"," +
                "\"progress\":51," +
                "\"color\":0," +
                "\"value\":3828.0," +
                "\"unit\":\"seconds\"" +
                "}" +
                "}";

        final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(json);

        assertNotNull(summaryData);

        ActivitySummaryEntry activitySummaryEntry = summaryData.get("test_progress");
        assertTrue(activitySummaryEntry instanceof ActivitySummaryProgressEntry);

        ActivitySummaryProgressEntry testProgress = (ActivitySummaryProgressEntry) activitySummaryEntry;

        assertEquals(3828, ((Number) testProgress.getValue()).doubleValue(), 0.000000001d);
        assertEquals("seconds", testProgress.getUnit());
        assertEquals(51, testProgress.getProgress());

        assertEquals(json, summaryData.toJson());
    }
}

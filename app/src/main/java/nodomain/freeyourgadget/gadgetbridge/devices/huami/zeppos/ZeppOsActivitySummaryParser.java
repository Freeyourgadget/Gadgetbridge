/*  Copyright (C) 2022-2024 José Rebelo, Reiner Herrmann

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import android.content.Context;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryProgressEntry;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.proto.HuamiProtos;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsActivityType;

public class ZeppOsActivitySummaryParser extends HuamiActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsActivitySummaryParser.class);
    private final Context context;

    @Override
    public AbstractHuamiActivityDetailsParser getDetailsParser(final BaseActivitySummary summary) {
        return new ZeppOsActivityDetailsParser(summary);
    }

    public ZeppOsActivitySummaryParser(final Context context) {
        this.context = context;
    }

    @Override
    protected void parseBinaryData(final BaseActivitySummary summary, final Date startTime) {
        final byte[] rawData = summary.getRawSummaryData();
        if (rawData == null) {
            return;
        }

        final int version = (rawData[0] & 0xff) | ((rawData[1] & 0xff) << 8);
        if (version != 0x8000) {
            LOG.warn("Unexpected binary data version {}, parsing might fail", version);
        }

        final byte[] protobufData = ArrayUtils.subarray(rawData, 2, rawData.length);
        final HuamiProtos.WorkoutSummary summaryProto;
        try {
            summaryProto = HuamiProtos.WorkoutSummary.parseFrom(protobufData);
        } catch (final InvalidProtocolBufferException e) {
            LOG.error("Failed to parse summary protobuf data", e);
            return;
        }

        if (summaryProto.hasType()) {
            final ZeppOsActivityType activityType = ZeppOsActivityType
                    .fromCode((byte) summaryProto.getType().getType());

            final ActivityKind activityKind;
            if (activityType != null) {
                activityKind = activityType.toActivityKind();
            } else {
                LOG.warn("Unknown workout activity type code {}", String.format("0x%X", summaryProto.getType().getType()));
                activityKind = ActivityKind.UNKNOWN;
            }
            summary.setActivityKind(activityKind.getCode());
        }

        if (summaryProto.hasTime()) {
            int totalDuration = summaryProto.getTime().getTotalDuration();
            summary.setEndTime(new Date(startTime.getTime() + totalDuration * 1000L));
            summaryData.add(ACTIVE_SECONDS, summaryProto.getTime().getWorkoutDuration(), UNIT_SECONDS);
            // TODO pause durations
        }

        if (summaryProto.hasLocation()) {
            summary.setBaseLongitude(summaryProto.getLocation().getBaseLongitude());
            summary.setBaseLatitude(summaryProto.getLocation().getBaseLatitude());
            summary.setBaseAltitude(summaryProto.getLocation().getBaseAltitude() / 2);
            // TODO: Min/Max Latitude/Longitude
            summaryData.add(ALTITUDE_BASE, summaryProto.getLocation().getBaseAltitude() / 2f, UNIT_METERS);
        }

        if (summaryProto.hasHeartRate()) {
            summaryData.add(HR_AVG, summaryProto.getHeartRate().getAvg(), UNIT_BPM);
            summaryData.add(HR_MAX, summaryProto.getHeartRate().getMax(), UNIT_BPM);
            summaryData.add(HR_MIN, summaryProto.getHeartRate().getMin(), UNIT_BPM);
        }

        if (summaryProto.hasSteps()) {
            summaryData.add(CADENCE_MAX, summaryProto.getSteps().getMaxCadence() * 60, UNIT_SPM);
            summaryData.add(CADENCE_AVG, summaryProto.getSteps().getAvgCadence() * 60, UNIT_SPM);
            summaryData.add(STRIDE_AVG, summaryProto.getSteps().getAvgStride(), UNIT_CM);
            summaryData.add(STEPS, summaryProto.getSteps().getSteps(), UNIT_STEPS);
        }

        if (summaryProto.hasDistance()) {
            summaryData.add(DISTANCE_METERS, summaryProto.getDistance().getDistance(), UNIT_METERS);
        }

        if (summaryProto.hasPace()) {
            summaryData.add(PACE_MAX, summaryProto.getPace().getBest(), UNIT_SECONDS_PER_M);
            summaryData.add(PACE_AVG_SECONDS_KM, summaryProto.getPace().getAvg() * 1000, UNIT_SECONDS_PER_KM);
        }

        if (summaryProto.hasCalories()) {
            summaryData.add(CALORIES_BURNT, summaryProto.getCalories().getCalories(), UNIT_KCAL);
        }

        if (summaryProto.hasHeartRateZones()) {
            // TODO hr zones bpm?
            if (summaryProto.getHeartRateZones().getZoneTimeCount() == 6) {
                final double totalTime = summaryProto.getHeartRateZones().getZoneTimeList()
                        .stream()
                        .mapToInt(v -> v)
                        .sum();

                final List<String> zoneOrder = Arrays.asList(HR_ZONE_NA, HR_ZONE_WARM_UP, HR_ZONE_FAT_BURN, HR_ZONE_AEROBIC, HR_ZONE_ANAEROBIC, HR_ZONE_EXTREME);
                final int[] zoneColors = new int[]{
                        0,
                        context.getResources().getColor(R.color.hr_zone_warm_up_color),
                        context.getResources().getColor(R.color.hr_zone_easy_color),
                        context.getResources().getColor(R.color.hr_zone_aerobic_color),
                        context.getResources().getColor(R.color.hr_zone_threshold_color),
                        context.getResources().getColor(R.color.hr_zone_maximum_color),
                };
                for (int i = 0; i < zoneOrder.size(); i++) {
                    summaryData.add(
                            zoneOrder.get(i),
                            new ActivitySummaryProgressEntry(
                                    summaryProto.getHeartRateZones().getZoneTime(i),
                                    UNIT_SECONDS,
                                    (int) ((100 * summaryProto.getHeartRateZones().getZoneTime(i)) / totalTime),
                                    zoneColors[i]
                            )
                    );
                }
            } else {
                LOG.warn("Unexpected number of HR zones {}", summaryProto.getHeartRateZones().getZoneTimeCount());
            }
        }

        if (summaryProto.hasTrainingEffect()) {
            summaryData.add(TRAINING_EFFECT_AEROBIC, summaryProto.getTrainingEffect().getAerobicTrainingEffect(), UNIT_NONE);
            summaryData.add(TRAINING_EFFECT_ANAEROBIC, summaryProto.getTrainingEffect().getAnaerobicTrainingEffect(), UNIT_NONE);
            summaryData.add(WORKOUT_LOAD, summaryProto.getTrainingEffect().getCurrentWorkoutLoad(), UNIT_NONE);
            summaryData.add(MAXIMUM_OXYGEN_UPTAKE, summaryProto.getTrainingEffect().getMaximumOxygenUptake(), UNIT_ML_KG_MIN);
        }

        if (summaryProto.hasAltitude()) {
            summaryData.add(ALTITUDE_MAX, summaryProto.getAltitude().getMaxAltitude() / 200f, UNIT_METERS);
            summaryData.add(ALTITUDE_MIN, summaryProto.getAltitude().getMinAltitude() / 200f, UNIT_METERS);
            summaryData.add(ALTITUDE_AVG, summaryProto.getAltitude().getAvgAltitude() / 200f, UNIT_METERS);
            // TODO totalClimbing
            summaryData.add(ELEVATION_GAIN, summaryProto.getAltitude().getElevationGain() / 100f, UNIT_METERS);
            summaryData.add(ELEVATION_LOSS, summaryProto.getAltitude().getElevationLoss() / 100f, UNIT_METERS);
        }

        if (summaryProto.hasElevation()) {
            summaryData.add(ASCENT_SECONDS, summaryProto.getElevation().getUphillTime(), UNIT_SECONDS);
            summaryData.add(DESCENT_SECONDS, summaryProto.getElevation().getDownhillTime(), UNIT_SECONDS);
        }

        if (summaryProto.hasSwimmingData()) {
            summaryData.add(LAPS, summaryProto.getSwimmingData().getLaps(), UNIT_LAPS);
            switch (summaryProto.getSwimmingData().getLaneLengthUnit()) {
                case 0:
                    summaryData.add(LANE_LENGTH, summaryProto.getSwimmingData().getLaneLength(), UNIT_METERS);
                    break;
                case 1:
                    summaryData.add(LANE_LENGTH, summaryProto.getSwimmingData().getLaneLength(), UNIT_YARD);
                    break;
            }
            switch (summaryProto.getSwimmingData().getStyle()) {
                // TODO i18n these
                case 1:
                    summaryData.add(SWIM_STYLE, "breaststroke");
                    break;
                case 2:
                    summaryData.add(SWIM_STYLE, "freestyle");
                    break;
            }
            summaryData.add(STROKES, summaryProto.getSwimmingData().getStrokes(), UNIT_STROKES);
            summaryData.add(STROKE_RATE_AVG, summaryProto.getSwimmingData().getAvgStrokeRate(), UNIT_STROKES_PER_MINUTE);
            summaryData.add(STROKE_RATE_MAX, summaryProto.getSwimmingData().getMaxStrokeRate(), UNIT_STROKES_PER_MINUTE);
            summaryData.add(STROKE_DISTANCE_AVG, summaryProto.getSwimmingData().getAvgDps(), UNIT_CM);
            summaryData.add(SWOLF_INDEX, summaryProto.getSwimmingData().getSwolf(), UNIT_NONE);
        }
    }
}

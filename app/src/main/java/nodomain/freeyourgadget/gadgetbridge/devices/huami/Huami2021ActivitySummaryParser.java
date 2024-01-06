/*  Copyright (C) 2022 José Rebelo

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

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.proto.HuamiProtos;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021ActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021WorkoutTrackActivityType;

public class Huami2021ActivitySummaryParser extends HuamiActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021ActivitySummaryParser.class);

    @Override
    public AbstractHuamiActivityDetailsParser getDetailsParser(final BaseActivitySummary summary) {
        return new Huami2021ActivityDetailsParser(summary);
    }

    @Override
    protected void parseBinaryData(final BaseActivitySummary summary, final Date startTime) {
        final byte[] rawData = summary.getRawSummaryData();
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
            final Huami2021WorkoutTrackActivityType workoutTrackActivityType = Huami2021WorkoutTrackActivityType
                    .fromCode((byte) summaryProto.getType().getType());

            final int activityKind;
            if (workoutTrackActivityType != null) {
                activityKind = workoutTrackActivityType.toActivityKind();
            } else {
                LOG.warn("Unknown workout activity type code {}", String.format("0x%X", summaryProto.getType().getType()));
                activityKind = ActivityKind.TYPE_UNKNOWN;
            }
            summary.setActivityKind(activityKind);
        }

        if (summaryProto.hasTime()) {
            int totalDuration = summaryProto.getTime().getTotalDuration();
            summary.setEndTime(new Date(startTime.getTime() + totalDuration * 1000L));
            addSummaryData(ACTIVE_SECONDS, summaryProto.getTime().getWorkoutDuration(), UNIT_SECONDS);
            // TODO pause durations
        }

        if (summaryProto.hasLocation()) {
            summary.setBaseLongitude(summaryProto.getLocation().getBaseLongitude());
            summary.setBaseLatitude(summaryProto.getLocation().getBaseLatitude());
            summary.setBaseAltitude(summaryProto.getLocation().getBaseAltitude() / 2);
            // TODO: Min/Max Latitude/Longitude
            addSummaryData(ALTITUDE_BASE, summaryProto.getLocation().getBaseAltitude() / 2, UNIT_METERS);
        }

        if (summaryProto.hasHeartRate()) {
            addSummaryData(HR_AVG, summaryProto.getHeartRate().getAvg(), UNIT_BPM);
            addSummaryData(HR_MAX, summaryProto.getHeartRate().getMax(), UNIT_BPM);
            addSummaryData(HR_MIN, summaryProto.getHeartRate().getMin(), UNIT_BPM);
        }

        if (summaryProto.hasSteps()) {
            addSummaryData(CADENCE_MAX, summaryProto.getSteps().getMaxCadence() * 60, UNIT_SPM);
            addSummaryData(CADENCE_AVG, summaryProto.getSteps().getAvgCadence() * 60, UNIT_SPM);
            addSummaryData(STRIDE_AVG, summaryProto.getSteps().getAvgStride(), UNIT_CM);
            addSummaryData(STEPS, summaryProto.getSteps().getSteps(), UNIT_STEPS);
        }

        if (summaryProto.hasDistance()) {
            addSummaryData(DISTANCE_METERS, summaryProto.getDistance().getDistance(), UNIT_METERS);
        }

        if (summaryProto.hasPace()) {
            addSummaryData(PACE_MAX, summaryProto.getPace().getBest(), UNIT_SECONDS_PER_M);
            addSummaryData(PACE_AVG_SECONDS_KM, summaryProto.getPace().getAvg() * 1000, UNIT_SECONDS_PER_KM);
        }

        if (summaryProto.hasCalories()) {
            addSummaryData(CALORIES_BURNT, summaryProto.getCalories().getCalories(), UNIT_KCAL);
        }

        if (summaryProto.hasHeartRateZones()) {
            // TODO hr zones bpm?
            if (summaryProto.getHeartRateZones().getZoneTimeCount() == 6) {
                addSummaryData(HR_ZONE_NA, summaryProto.getHeartRateZones().getZoneTime(0), UNIT_SECONDS);
                addSummaryData(HR_ZONE_WARM_UP, summaryProto.getHeartRateZones().getZoneTime(1), UNIT_SECONDS);
                addSummaryData(HR_ZONE_FAT_BURN, summaryProto.getHeartRateZones().getZoneTime(2), UNIT_SECONDS);
                addSummaryData(HR_ZONE_AEROBIC, summaryProto.getHeartRateZones().getZoneTime(3), UNIT_SECONDS);
                addSummaryData(HR_ZONE_ANAEROBIC, summaryProto.getHeartRateZones().getZoneTime(4), UNIT_SECONDS);
                addSummaryData(HR_ZONE_EXTREME, summaryProto.getHeartRateZones().getZoneTime(5), UNIT_SECONDS);
            } else {
                LOG.warn("Unexpected number of HR zones {}", summaryProto.getHeartRateZones().getZoneTimeCount());
            }
        }

        if (summaryProto.hasTrainingEffect()) {
            addSummaryData(TRAINING_EFFECT_AEROBIC, summaryProto.getTrainingEffect().getAerobicTrainingEffect(), UNIT_NONE);
            addSummaryData(TRAINING_EFFECT_ANAEROBIC, summaryProto.getTrainingEffect().getAnaerobicTrainingEffect(), UNIT_NONE);
            addSummaryData(WORKOUT_LOAD, summaryProto.getTrainingEffect().getCurrentWorkoutLoad(), UNIT_NONE);
            addSummaryData(MAXIMUM_OXYGEN_UPTAKE, summaryProto.getTrainingEffect().getMaximumOxygenUptake(), UNIT_ML_KG_MIN);
        }

        if (summaryProto.hasAltitude()) {
            addSummaryData(ALTITUDE_MAX, summaryProto.getAltitude().getMaxAltitude() / 200, UNIT_METERS);
            addSummaryData(ALTITUDE_MIN, summaryProto.getAltitude().getMinAltitude() / 200, UNIT_METERS);
            addSummaryData(ALTITUDE_AVG, summaryProto.getAltitude().getAvgAltitude() / 200, UNIT_METERS);
            // TODO totalClimbing
            addSummaryData(ELEVATION_GAIN, summaryProto.getAltitude().getElevationGain() / 100, UNIT_METERS);
            addSummaryData(ELEVATION_LOSS, summaryProto.getAltitude().getElevationLoss() / 100, UNIT_METERS);
        }

        if (summaryProto.hasElevation()) {
            addSummaryData(ASCENT_SECONDS, summaryProto.getElevation().getUphillTime(), UNIT_SECONDS);
            addSummaryData(DESCENT_SECONDS, summaryProto.getElevation().getDownhillTime(), UNIT_SECONDS);
        }

        if (summaryProto.hasSwimmingData()) {
            addSummaryData(LAPS, summaryProto.getSwimmingData().getLaps(), UNIT_LAPS);
            switch (summaryProto.getSwimmingData().getLaneLengthUnit()) {
                case 0:
                    addSummaryData(LANE_LENGTH, summaryProto.getSwimmingData().getLaneLength(), UNIT_METERS);
                    break;
                case 1:
                    addSummaryData(LANE_LENGTH, summaryProto.getSwimmingData().getLaneLength(), UNIT_YARD);
                    break;
            }
            switch (summaryProto.getSwimmingData().getStyle()) {
                // TODO i18n these
                case 1:
                    addSummaryData(SWIM_STYLE, "breaststroke");
                    break;
                case 2:
                    addSummaryData(SWIM_STYLE, "freestyle");
                    break;
            }
            addSummaryData(STROKES, summaryProto.getSwimmingData().getStrokes(), UNIT_STROKES);
            addSummaryData(STROKE_RATE_AVG, summaryProto.getSwimmingData().getAvgStrokeRate(), UNIT_STROKES_PER_MINUTE);
            addSummaryData(STROKE_RATE_MAX, summaryProto.getSwimmingData().getMaxStrokeRate(), UNIT_STROKES_PER_MINUTE);
            addSummaryData(STROKE_DISTANCE_AVG, summaryProto.getSwimmingData().getAvgDps(), UNIT_CM);
            addSummaryData(SWOLF_INDEX, summaryProto.getSwimmingData().getSwolf(), UNIT_NONE);
        }
    }
}

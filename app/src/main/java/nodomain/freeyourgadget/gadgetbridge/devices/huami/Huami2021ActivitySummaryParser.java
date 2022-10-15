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
            addSummaryData("activeSeconds", summaryProto.getTime().getWorkoutDuration(), "seconds");
            // TODO pause durations
        }

        if (summaryProto.hasLocation()) {
            summary.setBaseLongitude(summaryProto.getLocation().getBaseLongitude());
            summary.setBaseLatitude(summaryProto.getLocation().getBaseLatitude());
            summary.setBaseAltitude(summaryProto.getLocation().getBaseAltitude() / 2);
            // TODO: Min/Max Latitude/Longitude
            addSummaryData("baseAltitude", summaryProto.getLocation().getBaseAltitude() / 2, "meters");
        }

        if (summaryProto.hasHeartRate()) {
            addSummaryData("averageHR", summaryProto.getHeartRate().getAvg(), "bpm");
            addSummaryData("maxHR", summaryProto.getHeartRate().getMax(), "bpm");
            addSummaryData("minHR", summaryProto.getHeartRate().getMin(), "bpm");
        }

        if (summaryProto.hasSteps()) {
            addSummaryData("maxCadence", summaryProto.getSteps().getMaxCadence() * 60, "spm");
            addSummaryData("averageCadence", summaryProto.getSteps().getAvgCadence() * 60, "spm");
            addSummaryData("averageStride", summaryProto.getSteps().getAvgStride(), "cm");
            addSummaryData("steps", summaryProto.getSteps().getSteps(), "steps_unit");
        }

        if (summaryProto.hasDistance()) {
            addSummaryData("distanceMeters", summaryProto.getDistance().getDistance(), "meters");
        }

        if (summaryProto.hasPace()) {
            addSummaryData("maxPace", summaryProto.getPace().getBest(), "seconds_m");
            addSummaryData("averageKMPaceSeconds", summaryProto.getPace().getAvg() * 1000, "seconds_km");
        }

        if (summaryProto.hasCalories()) {
            addSummaryData("caloriesBurnt", summaryProto.getCalories().getCalories(), "calories_unit");
        }

        if (summaryProto.hasHeartRateZones()) {
            // TODO hr zones bpm?
            if (summaryProto.getHeartRateZones().getZoneTimeCount() == 6) {
                addSummaryData("hrZoneNa", summaryProto.getHeartRateZones().getZoneTime(0), "seconds");
                addSummaryData("hrZoneWarmUp", summaryProto.getHeartRateZones().getZoneTime(1), "seconds");
                addSummaryData("hrZoneFatBurn", summaryProto.getHeartRateZones().getZoneTime(2), "seconds");
                addSummaryData("hrZoneAerobic", summaryProto.getHeartRateZones().getZoneTime(3), "seconds");
                addSummaryData("hrZoneAnaerobic", summaryProto.getHeartRateZones().getZoneTime(4), "seconds");
                addSummaryData("hrZoneExtreme", summaryProto.getHeartRateZones().getZoneTime(5), "seconds");
            } else {
                LOG.warn("Unexpected number of HR zones {}", summaryProto.getHeartRateZones().getZoneTimeCount());
            }
        }

        if (summaryProto.hasTrainingEffect()) {
            addSummaryData("aerobicTrainingEffect", summaryProto.getTrainingEffect().getAerobicTrainingEffect(), "");
            addSummaryData("anaerobicTrainingEffect", summaryProto.getTrainingEffect().getAnaerobicTrainingEffect(), "");
            addSummaryData("currentWorkoutLoad", summaryProto.getTrainingEffect().getCurrentWorkoutLoad(), "");
            addSummaryData("maximumOxygenUptake", summaryProto.getTrainingEffect().getMaximumOxygenUptake(), "ml/kg/min");
        }

        if (summaryProto.hasAltitude()) {
            addSummaryData("maxAltitude", summaryProto.getAltitude().getMaxAltitude() / 200, "meters");
            addSummaryData("minAltitude", summaryProto.getAltitude().getMinAltitude() / 200, "meters");
            addSummaryData("averageAltitude", summaryProto.getAltitude().getAvgAltitude() / 200, "meters");
            // TODO totalClimbing
            addSummaryData("elevationGain", summaryProto.getAltitude().getElevationGain() / 100, "meters");
            addSummaryData("elevationLoss", summaryProto.getAltitude().getElevationLoss() / 100, "meters");
        }

        if (summaryProto.hasElevation()) {
            addSummaryData("ascentSeconds", summaryProto.getElevation().getUphillTime(), "seconds");
            addSummaryData("descentSeconds", summaryProto.getElevation().getDownhillTime(), "seconds");
        }
    }
}

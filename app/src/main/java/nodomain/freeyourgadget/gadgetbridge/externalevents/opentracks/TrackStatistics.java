/*  Copyright (C) 2022-2024 José Rebelo

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

package nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks;

import java.util.List;

/**
 * This class was copied and modified from
 * https://github.com/OpenTracksApp/OSMDashboard/blob/main/src/main/java/de/storchp/opentracks/osmplugin/utils/TrackStatistics.java
 */
class TrackStatistics {
    private String category = "unknown";
    private int startTimeEpochMillis;
    private int stopTimeEpochMillis;
    private float totalDistanceMeter;
    private int totalTimeMillis;
    private int movingTimeMillis;
    private float avgSpeedMeterPerSecond;
    private float avgMovingSpeedMeterPerSecond;
    private float maxSpeedMeterPerSecond;
    private float minElevationMeter;
    private float maxElevationMeter;
    private float elevationGainMeter;

    public TrackStatistics(final List<Track> tracks) {
        if (tracks.isEmpty()) {
            return;
        }
        final Track first = tracks.get(0);
        category = first.getCategory();
        startTimeEpochMillis = first.getStartTimeEpochMillis();
        stopTimeEpochMillis = first.getStopTimeEpochMillis();
        totalDistanceMeter = first.getTotalDistanceMeter();
        totalTimeMillis = first.getTotalTimeMillis();
        movingTimeMillis = first.getMovingTimeMillis();
        avgSpeedMeterPerSecond = first.getAvgSpeedMeterPerSecond();
        avgMovingSpeedMeterPerSecond = first.getAvgMovingSpeedMeterPerSecond();
        maxSpeedMeterPerSecond = first.getMaxSpeedMeterPerSecond();
        minElevationMeter = first.getMinElevationMeter();
        maxElevationMeter = first.getMaxElevationMeter();
        elevationGainMeter = first.getElevationGainMeter();

        if (tracks.size() > 1) {
            float totalAvgSpeedMeterPerSecond = avgSpeedMeterPerSecond;
            float totalAvgMovingSpeedMeterPerSecond = avgMovingSpeedMeterPerSecond;
            for (final Track track : tracks.subList(1, tracks.size())) {
                if (!category.equals(track.getCategory())) {
                    category = "mixed";
                }
                startTimeEpochMillis = Math.min(startTimeEpochMillis, track.getStartTimeEpochMillis());
                stopTimeEpochMillis = Math.max(stopTimeEpochMillis, track.getStopTimeEpochMillis());
                totalDistanceMeter += track.getTotalDistanceMeter();
                totalTimeMillis += track.getTotalTimeMillis();
                movingTimeMillis += track.getMovingTimeMillis();
                totalAvgSpeedMeterPerSecond += track.getAvgSpeedMeterPerSecond();
                totalAvgMovingSpeedMeterPerSecond += track.getAvgMovingSpeedMeterPerSecond();
                maxSpeedMeterPerSecond = Math.max(maxSpeedMeterPerSecond, track.getMaxSpeedMeterPerSecond());
                minElevationMeter = Math.min(minElevationMeter, track.getMinElevationMeter());
                maxElevationMeter = Math.max(maxElevationMeter, track.getMaxElevationMeter());
                elevationGainMeter += track.getElevationGainMeter();
            }

            avgSpeedMeterPerSecond = totalAvgSpeedMeterPerSecond / tracks.size();
            avgMovingSpeedMeterPerSecond = totalAvgMovingSpeedMeterPerSecond / tracks.size();
        }
    }

    public String getCategory() {
        return category;
    }

    public int getStartTimeEpochMillis() {
        return startTimeEpochMillis;
    }

    public int getStopTimeEpochMillis() {
        return stopTimeEpochMillis;
    }

    public float getTotalDistanceMeter() {
        return totalDistanceMeter;
    }

    public int getTotalTimeMillis() {
        return totalTimeMillis;
    }

    public int getMovingTimeMillis() {
        return movingTimeMillis;
    }

    public float getAvgSpeedMeterPerSecond() {
        return avgSpeedMeterPerSecond;
    }

    public float getAvgMovingSpeedMeterPerSecond() {
        return avgMovingSpeedMeterPerSecond;
    }

    public float getMaxSpeedMeterPerSecond() {
        return maxSpeedMeterPerSecond;
    }

    public float getMinElevationMeter() {
        return minElevationMeter;
    }

    public float getMaxElevationMeter() {
        return maxElevationMeter;
    }

    public float getElevationGainMeter() {
        return elevationGainMeter;
    }
}

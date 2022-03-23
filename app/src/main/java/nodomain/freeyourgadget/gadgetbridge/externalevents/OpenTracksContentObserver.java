/*  Copyright (C) 2022 Arjan Schrijver

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

package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class OpenTracksContentObserver extends ContentObserver {
    private Context mContext;
    private Uri tracksUri;
    private int protocolVersion;
    private int totalTimeMillis;
    private float totalDistanceMeter;

    private long previousTimeMillis = 0;
    private float previousDistanceMeter = 0;

    public int getTotalTimeMillis() {
        return totalTimeMillis;
    }
    public float getTotalDistanceMeter() {
        return totalDistanceMeter;
    }

    public long getTimeMillisChange() {
        /**
         * We don't use the timeMillis received from OpenTracks here, because those updates do not
         * come in very regularly when GPS reception is bad
         */
        long timeMillisDelta = System.currentTimeMillis() - previousTimeMillis;
        previousTimeMillis = System.currentTimeMillis();
        return timeMillisDelta;
    }

    public float getDistanceMeterChange() {
        float distanceMeterDelta = totalDistanceMeter - previousDistanceMeter;
        previousDistanceMeter = totalDistanceMeter;
        return distanceMeterDelta;
    }


    public OpenTracksContentObserver(Context context, final Uri tracksUri, final int protocolVersion) {
        super(new Handler());
        this.mContext = context;
        this.tracksUri = tracksUri;
        this.protocolVersion = protocolVersion;
        this.previousTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void onChange(final boolean selfChange, final Uri uri) {
        if (uri == null) {
            return; // nothing can be done without an uri
        }
        if (tracksUri.toString().startsWith(uri.toString())) {
            final List<Track> tracks = Track.readTracks(mContext.getContentResolver(), tracksUri, protocolVersion);
            if (!tracks.isEmpty()) {
                final TrackStatistics statistics = new TrackStatistics(tracks);
                totalTimeMillis = statistics.getTotalTimeMillis();
                totalDistanceMeter = statistics.getTotalDistanceMeter();
            }
        }
    }

    public void unregister() {
        if (mContext != null) {
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    public void finish() {
        unregister();
        if (mContext != null) {
            ((Activity) mContext).finish();
            mContext = null;
        }
    }
}

class Track {
    /**
     * This class was copied and modified from
     * https://github.com/OpenTracksApp/OSMDashboard/blob/main/src/main/java/de/storchp/opentracks/osmplugin/dashboardapi/Track.java
     */
    private static final Logger LOG = LoggerFactory.getLogger(Track.class);

    private static final String TAG = Track.class.getSimpleName();

    public static final String _ID = "_id";
    public static final String NAME = "name"; // track name
    public static final String DESCRIPTION = "description"; // track description
    public static final String CATEGORY = "category"; // track activity type
    public static final String STARTTIME = "starttime"; // track start time
    public static final String STOPTIME = "stoptime"; // track stop time
    public static final String TOTALDISTANCE = "totaldistance"; // total distance
    public static final String TOTALTIME = "totaltime"; // total time
    public static final String MOVINGTIME = "movingtime"; // moving time
    public static final String AVGSPEED = "avgspeed"; // average speed
    public static final String AVGMOVINGSPEED = "avgmovingspeed"; // average moving speed
    public static final String MAXSPEED = "maxspeed"; // maximum speed
    public static final String MINELEVATION = "minelevation"; // minimum elevation
    public static final String MAXELEVATION = "maxelevation"; // maximum elevation
    public static final String ELEVATIONGAIN = "elevationgain"; // elevation gain

    public static final String[] PROJECTION = {
            _ID,
            NAME,
            DESCRIPTION,
            CATEGORY,
            STARTTIME,
            STOPTIME,
            TOTALDISTANCE,
            TOTALTIME,
            MOVINGTIME,
            AVGSPEED,
            AVGMOVINGSPEED,
            MAXSPEED,
            MINELEVATION,
            MAXELEVATION,
            ELEVATIONGAIN
    };

    private final long id;
    private final String trackname;
    private final String description;
    private final String category;
    private final int startTimeEpochMillis;
    private final int stopTimeEpochMillis;
    private final float totalDistanceMeter;
    private final int totalTimeMillis;
    private final int movingTimeMillis;
    private final float avgSpeedMeterPerSecond;
    private final float avgMovingSpeedMeterPerSecond;
    private final float maxSpeedMeterPerSecond;
    private final float minElevationMeter;
    private final float maxElevationMeter;
    private final float elevationGainMeter;

    public Track(final long id, final String trackname, final String description, final String category, final int startTimeEpochMillis, final int stopTimeEpochMillis, final float totalDistanceMeter, final int totalTimeMillis, final int movingTimeMillis, final float avgSpeedMeterPerSecond, final float avgMovingSpeedMeterPerSecond, final float maxSpeedMeterPerSecond, final float minElevationMeter, final float maxElevationMeter, final float elevationGainMeter) {
        this.id = id;
        this.trackname = trackname;
        this.description = description;
        this.category = category;
        this.startTimeEpochMillis = startTimeEpochMillis;
        this.stopTimeEpochMillis = stopTimeEpochMillis;
        this.totalDistanceMeter = totalDistanceMeter;
        this.totalTimeMillis = totalTimeMillis;
        this.movingTimeMillis = movingTimeMillis;
        this.avgSpeedMeterPerSecond = avgSpeedMeterPerSecond;
        this.avgMovingSpeedMeterPerSecond = avgMovingSpeedMeterPerSecond;
        this.maxSpeedMeterPerSecond = maxSpeedMeterPerSecond;
        this.minElevationMeter = minElevationMeter;
        this.maxElevationMeter = maxElevationMeter;
        this.elevationGainMeter = elevationGainMeter;
    }

    /**
     * Reads the Tracks from the Content Uri
     */
    public static List<Track> readTracks(final ContentResolver resolver, final Uri data, final int protocolVersion) {
        LOG.info("Loading track(s) from " + data);

        final ArrayList<Track> tracks = new ArrayList<Track>();
        try (final Cursor cursor = resolver.query(data, Track.PROJECTION, null, null, null)) {
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(Track._ID));
                final String trackname = cursor.getString(cursor.getColumnIndexOrThrow(Track.NAME));
                final String description = cursor.getString(cursor.getColumnIndexOrThrow(Track.DESCRIPTION));
                final String category = cursor.getString(cursor.getColumnIndexOrThrow(Track.CATEGORY));
                final int startTimeEpochMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.STARTTIME));
                final int stopTimeEpochMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.STOPTIME));
                final float totalDistanceMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.TOTALDISTANCE));
                final int totalTimeMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.TOTALTIME));
                final int movingTimeMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.MOVINGTIME));
                final float avgSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.AVGSPEED));
                final float avgMovingSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.AVGMOVINGSPEED));
                final float maxSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MAXSPEED));
                final float minElevationMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MINELEVATION));
                final float maxElevationMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MAXELEVATION));
                final float elevationGainMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.ELEVATIONGAIN));

                LOG.info("New Track data received: distance=" + totalDistanceMeter + " time=" + totalTimeMillis);

                tracks.add(new Track(id, trackname, description, category, startTimeEpochMillis, stopTimeEpochMillis,
                        totalDistanceMeter, totalTimeMillis, movingTimeMillis, avgSpeedMeterPerSecond, avgMovingSpeedMeterPerSecond, maxSpeedMeterPerSecond,
                        minElevationMeter, maxElevationMeter, elevationGainMeter));
            }
        } catch (final SecurityException e) {
            LOG.warn("No permission to read track", e);
        } catch (final Exception e) {
            LOG.warn("Reading track failed", e);
        }
        return tracks;
    }

    public float getElevationGainMeter() {
        return elevationGainMeter;
    }

    public float getMaxElevationMeter() {
        return maxElevationMeter;
    }

    public float getMinElevationMeter() {
        return minElevationMeter;
    }

    public float getMaxSpeedMeterPerSecond() {
        return maxSpeedMeterPerSecond;
    }

    public float getAvgMovingSpeedMeterPerSecond() {
        return avgMovingSpeedMeterPerSecond;
    }

    public float getAvgSpeedMeterPerSecond() {
        return avgSpeedMeterPerSecond;
    }

    public int getMovingTimeMillis() {
        return movingTimeMillis;
    }

    public int getTotalTimeMillis() {
        return totalTimeMillis;
    }

    public float getTotalDistanceMeter() {
        return totalDistanceMeter;
    }

    public int getStopTimeEpochMillis() {
        return stopTimeEpochMillis;
    }

    public int getStartTimeEpochMillis() {
        return startTimeEpochMillis;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getTrackname() {
        return trackname;
    }

    public long getId() {
        return id;
    }
}

class TrackStatistics {
    /**
     * This class was copied and modified from
     * https://github.com/OpenTracksApp/OSMDashboard/blob/main/src/main/java/de/storchp/opentracks/osmplugin/utils/TrackStatistics.java
     */

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

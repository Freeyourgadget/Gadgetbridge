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

package nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

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


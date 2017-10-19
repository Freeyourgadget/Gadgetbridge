package nodomain.freeyourgadget.gadgetbridge.model;

import android.support.annotation.Nullable;

import java.util.Date;

// https://www8.garmin.com/xmlschemas/TrackPointExtensionv1.xsd
/*
<trkpt lat="54.8591470" lon="-1.5754310">
        <ele>29.2</ele>
        <time>2015-07-26T07:43:42Z</time>
        <extensions>
        <gpxtpx:TrackPointExtension>
        <gpxtpx:atemp>11</gpxtpx:atemp>
        <gpxtpx:hr>92</gpxtpx:hr>
        <gpxtpx:cad>0</gpxtpx:cad>
        </gpxtpx:TrackPointExtension>
        </extensions>
        </trkpt>
*/
public class ActivityPoint {
    private Date time;
    private GPSCoordinate location;
    private int heartRate;
    private long speed4;
    private long speed5;
    private long speed6;

    // e.g. to describe a pause during the activity
    private @Nullable String description;

    public ActivityPoint() {
    }

    public ActivityPoint(Date time) {
        this.time = time;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public GPSCoordinate getLocation() {
        return location;
    }

    public void setLocation(GPSCoordinate location) {
        this.location = location;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public long getSpeed4() {
        return speed4;
    }

    public void setSpeed4(long speed4) {
        this.speed4 = speed4;
    }

    public long getSpeed5() {
        return speed5;
    }

    public void setSpeed5(long speed5) {
        this.speed5 = speed5;
    }

    public long getSpeed6() {
        return speed6;
    }

    public void setSpeed6(long speed6) {
        this.speed6 = speed6;
    }
}

package nodomain.freeyourgadget.gadgetbridge.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Summarized information about a temporal activity.
 *
 * // TODO: split into separate entities?
 */
public interface ActivitySummary extends Serializable {
    String getName();
    Date getStartTime();
    Date getEndTime();

    int getActivityKind();
    String getGpxTrack();

    long getDeviceId();

    long getUserId();
    //    long getSteps();
//    float getDistanceMeters();
//    float getAscentMeters();
//    float getDescentMeters();
//    float getMinAltitude();
//    float getMaxAltitude();
//    float getCalories();
//
//    float getMaxSpeed();
//    float getMinSpeed();
//    float getAverageSpeed();
}

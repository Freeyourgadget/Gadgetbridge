/*  Copyright (C) 2020-2024 Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.io.Serializable;
import java.util.Date;

// ActivitySession holds activities detected by the steps/hr/intensity
// and is used in the Activity List
public class ActivitySession implements Serializable {

    public static int SESSION_NORMAL = 1;
    public static int SESSION_SUMMARY = 2;
    public static int SESSION_ONGOING = 3;
    public static int SESSION_EMPTY = 4;

    private final Date startTime;
    private final Date endTime;
    private final int activeSteps;
    private final int heartRateAverage;
    private final float intensity;
    private final float distance;
    private final ActivityKind activityKind;
    // following is related to step session, we hold it here for the listview
    // it is identified by SESSION_SUMMARY
    private int sessionCount = 0;
    private int sessionType = SESSION_NORMAL;
    private boolean isEmptySummary = false; // in case there is no activity on that day
    private int totalDaySteps;


    public ActivitySession(Date startTime,
                           Date endTime,
                           int steps, int heartRateAverage, float intensity, float distance, ActivityKind activityKind) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.activeSteps = steps;
        this.heartRateAverage = heartRateAverage;
        this.intensity = intensity;
        this.distance = distance;
        this.activityKind = activityKind;
    }

    public ActivitySession(){
        this.startTime = null;
        this.endTime = null;
        this.activeSteps=0;
        this.heartRateAverage = 0;
        this.intensity = 0;
        this.distance = 0;
        this.activityKind = ActivityKind.UNKNOWN;
    };

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getActiveSteps() {
        return activeSteps;
    }

    public int getHeartRateAverage() {
        return heartRateAverage;
    }

    public ActivityKind getActivityKind() {
        return activityKind;
    }

    public float getIntensity() {
        return intensity;
    }

    public float getDistance() {
        return distance;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public int getSessionType() {
        return sessionType;
    }

    public void setSessionType(int sessionType) {
        this.sessionType = sessionType;
    }

    public boolean getIsEmptySummary() {
        return isEmptySummary;
    }

    public void setEmptySummary(boolean emptySummary) {
        this.isEmptySummary = emptySummary;
    }

    public int getTotalDaySteps() {
        return totalDaySteps;
    }

    public void setTotalDaySteps(int totalDaySteps) {
        this.totalDaySteps = totalDaySteps;
    }
}

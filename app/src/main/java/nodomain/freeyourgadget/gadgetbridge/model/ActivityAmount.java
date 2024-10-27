/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Pavel Elagin

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

import java.util.Date;

public class ActivityAmount {
    private final ActivityKind activityKind;
    private short percent;
    private long totalSeconds;
    private long totalSteps;
    private long totalDistance;
    private long totalActiveCalories;
    private Date startDate = null;
    private Date endDate = null;

    public ActivityAmount(ActivityKind activityKind) {
        this.activityKind = activityKind;
    }

    public void addSeconds(long seconds) {
        totalSeconds += seconds;
    }

    public void addSteps(long steps) {
        totalSteps += steps;
    }

    public void addDistance(long distance) {
        totalDistance += distance;
    }

    public void addActiveCalories(long activeCalories) {
        totalActiveCalories += activeCalories;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public long getTotalSteps() {
        return totalSteps;
    }

    public long getTotalDistance() {
        return totalDistance;
    }

    public long getTotalActiveCalories() {
        return totalActiveCalories;
    }

    public ActivityKind getActivityKind() {
        return activityKind;
    }

    public short getPercent() {
        return percent;
    }

    public void setPercent(short percent) {
        this.percent = percent;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(int seconds) {
        if(startDate == null)
            this.startDate = new Date((long)seconds * 1000);
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(int seconds) {
        this.endDate = new Date((long)seconds * 1000);
    }
}

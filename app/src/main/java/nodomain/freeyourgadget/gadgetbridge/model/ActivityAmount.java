/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.R;

public class ActivityAmount {
    private final int activityKind;
    private short percent;
    private long totalSeconds;
    private long totalSteps;

    public ActivityAmount(int activityKind) {
        this.activityKind = activityKind;
    }

    public void addSeconds(long seconds) {
        totalSeconds += seconds;
    }

    public void addSteps(long steps) {
        totalSteps += steps;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public long getTotalSteps() {
        return totalSteps;
    }

    public int getActivityKind() {
        return activityKind;
    }

    public short getPercent() {
        return percent;
    }

    public void setPercent(short percent) {
        this.percent = percent;
    }

    public String getName(Context context) {
        switch (activityKind) {
            case ActivityKind.TYPE_DEEP_SLEEP:
                return context.getString(R.string.abstract_chart_fragment_kind_deep_sleep);
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return context.getString(R.string.abstract_chart_fragment_kind_light_sleep);
        }
        return context.getString(R.string.abstract_chart_fragment_kind_activity);
    }
}

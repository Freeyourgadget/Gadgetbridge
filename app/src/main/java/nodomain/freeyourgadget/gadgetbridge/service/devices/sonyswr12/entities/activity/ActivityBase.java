/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public abstract class ActivityBase {
    protected final ActivityType type;
    private final long timeStampSec;

    public ActivityBase(ActivityType type, int timeOffsetMin, long timeStampSec) {
        if (timeOffsetMin < 0 || timeOffsetMin > 1440) {
            throw new IllegalArgumentException("activity time offset out of range: " + timeOffsetMin);
        }
        this.type = type;
        this.timeStampSec = timeStampSec + timeOffsetMin * 60;
    }

    public final int getTimeStampSec() {
        return (int) (timeStampSec);
    }

    public final ActivityType getType() {
        return this.type;
    }
}
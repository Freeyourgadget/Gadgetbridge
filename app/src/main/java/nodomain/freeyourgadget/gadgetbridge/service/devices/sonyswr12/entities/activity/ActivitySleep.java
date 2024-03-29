/*  Copyright (C) 2020-2024 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public class ActivitySleep extends ActivityBase {
    public final SleepLevel sleepLevel;
    public final int durationMin;

    public ActivitySleep(int timeOffsetMin, int durationMin, SleepLevel sleepLevel, Long timeStampSec) {
        super(ActivityType.SLEEP, timeOffsetMin, timeStampSec);
        this.durationMin = durationMin;
        this.sleepLevel = sleepLevel;
    }
}
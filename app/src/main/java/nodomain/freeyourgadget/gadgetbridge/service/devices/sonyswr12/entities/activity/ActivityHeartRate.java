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

public class ActivityHeartRate extends ActivityBase {
    public final int bpm;

    public ActivityHeartRate(int timeOffsetMin, int bpm, Long timeStampSec) {
        super(ActivityType.HEART_RATE, timeOffsetMin, timeStampSec);
        if (bpm < 0 || bpm > 65535) {
            throw new IllegalArgumentException("bpm out of range: " + bpm);
        }
        this.bpm = bpm;
    }
}
/*  Copyright (C) 2024 José Rebelo

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

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.entities.AbstractTimeSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public abstract class SleepScoreSample extends AbstractTimeSample {
    public abstract int getSleepScore();

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "timestamp=" + DateTimeUtils.formatDateTime(DateTimeUtils.parseTimestampMillis(getTimestamp())) +
                ", userId=" + getUserId() +
                ", deviceId=" + getDeviceId() +
                ", sleepScore=" + getSleepScore() +
                "}";
    }
}

/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.entities;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public abstract class AbstractSpo2Sample extends AbstractTimeSample implements Spo2Sample {
    public int getTypeNum() {
        return Spo2Sample.Type.UNKNOWN.getNum();
    }

    @Override
    public Type getType() {
        return Type.fromNum(getTypeNum());
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "timestamp=" + DateTimeUtils.formatDateTime(DateTimeUtils.parseTimestampMillis(getTimestamp())) +
                ", spo2=" + getSpo2() +
                ", type=" + getType() +
                ", userId=" + getUserId() +
                ", deviceId=" + getDeviceId() +
                "}";
    }
}

/*  Copyright (C) 2016-2017 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;

public class TrailingActivitySample extends AbstractActivitySample {
    private int timestamp;
    private long userId;
    private long deviceId;

    @Override
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Override
    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public long getDeviceId() {
        return deviceId;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }
}

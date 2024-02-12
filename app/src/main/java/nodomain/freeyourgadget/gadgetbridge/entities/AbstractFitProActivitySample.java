/*  Copyright (C) 2021-2024 Petr Vaněk

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

public abstract class AbstractFitProActivitySample extends AbstractActivitySample {

    abstract public int getSteps();

    @Override
    public int getRawIntensity() {
        return getSteps();
    }

    @Override
    public void setTimestamp(int timestamp) {

    }

    @Override
    public void setUserId(long userId) {

    }

    @Override
    public void setDeviceId(long deviceId) {

    }

    @Override
    public long getDeviceId() {
        return 0;
    }

    @Override
    public long getUserId() {
        return 0;
    }

    @Override
    public int getTimestamp() {
        return 0;
    }
}




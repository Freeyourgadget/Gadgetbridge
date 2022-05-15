/*  Copyright (C) 2015-2021 Petr Vaněk

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

import java.util.Objects;

public class BatteryConfig {

    private final int batteryIndex;
    private final int batteryIcon;
    private final int batteryLabel;

    public BatteryConfig(int batteryIndex, int batteryIcon, int batteryLabel) {
        this.batteryIndex = batteryIndex;
        this.batteryIcon = batteryIcon;
        this.batteryLabel = batteryLabel;
    }

    public int getBatteryIndex() {
        return batteryIndex;
    }

    public int getBatteryIcon() {
        return batteryIcon;
    }

    public int getBatteryLabel() {
        return batteryLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BatteryConfig)) return false;
        BatteryConfig that = (BatteryConfig) o;
        return getBatteryIndex() == that.getBatteryIndex() && getBatteryIcon() == that.getBatteryIcon() && getBatteryLabel() == that.getBatteryLabel();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBatteryIndex(), getBatteryIcon(), getBatteryLabel());
    }
}

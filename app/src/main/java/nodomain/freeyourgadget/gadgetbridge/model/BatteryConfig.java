/*  Copyright (C) 2021-2024 José Rebelo, Petr Vaněk

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

import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class BatteryConfig {
    private final int batteryIndex;
    private final int batteryIcon;
    private final int batteryLabel;
    private final int defaultLowThreshold;
    private final int defaultFullThreshold;

    public BatteryConfig(int batteryIndex) {
        this(batteryIndex, GBDevice.BATTERY_ICON_DEFAULT, GBDevice.BATTERY_LABEL_DEFAULT);
    }

    public BatteryConfig(int batteryIndex, int batteryIcon, int batteryLabel) {
        this(batteryIndex, batteryIcon, batteryLabel, 10, 100);
    }

    public BatteryConfig(final int batteryIndex, final int batteryIcon, final int batteryLabel, final int defaultLowThreshold, final int defaultFullThreshold) {
        this.batteryIndex = batteryIndex;
        this.batteryIcon = batteryIcon;
        this.batteryLabel = batteryLabel;
        this.defaultLowThreshold = defaultLowThreshold;
        this.defaultFullThreshold = defaultFullThreshold;
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

    public int getDefaultLowThreshold() {
        return defaultLowThreshold;
    }

    public int getDefaultFullThreshold() {
        return defaultFullThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BatteryConfig)) return false;
        BatteryConfig that = (BatteryConfig) o;
        return getBatteryIndex() == that.getBatteryIndex() &&
                getBatteryIcon() == that.getBatteryIcon() &&
                getBatteryLabel() == that.getBatteryLabel() &&
                getDefaultLowThreshold() == that.getDefaultLowThreshold() &&
                getDefaultFullThreshold() == that.getDefaultFullThreshold();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getBatteryIndex(),
                getBatteryIcon(),
                getBatteryLabel(),
                getDefaultLowThreshold(),
                getDefaultFullThreshold()
        );
    }
}

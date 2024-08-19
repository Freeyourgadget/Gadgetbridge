/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings;

public enum MoyoungEnumMetricSystem implements MoyoungEnum {
    METRIC_SYSTEM((byte)0),
    IMPERIAL_SYSTEM((byte)1);

    public final byte value;

    MoyoungEnumMetricSystem(byte value) {
        this.value = value;
    }

    @Override
    public byte value() {
        return value;
    }
}

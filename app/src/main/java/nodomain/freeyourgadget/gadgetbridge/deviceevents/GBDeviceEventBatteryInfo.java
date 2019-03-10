/*  Copyright (C) 2015-2019 Andreas Shimokawa, Daniele Gobbetti, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;


import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;

public class GBDeviceEventBatteryInfo extends GBDeviceEvent {
    public GregorianCalendar lastChargeTime = null;
    public BatteryState state = BatteryState.UNKNOWN;
    public short level = 50;
    public int numCharges = -1;
    public float voltage = -1f;

    public boolean extendedInfoAvailable() {
        if (numCharges != -1 && lastChargeTime != null) {
            return true;
        }
        return false;
    }
}

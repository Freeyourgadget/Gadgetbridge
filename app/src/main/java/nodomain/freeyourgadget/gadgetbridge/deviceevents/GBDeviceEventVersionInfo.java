/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer

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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class GBDeviceEventVersionInfo extends GBDeviceEvent {
    public String fwVersion = "N/A";
    public String hwVersion = "N/A";

    public GBDeviceEventVersionInfo() {
        if (GBApplication.getContext() != null) {
            // Only get from context if there is one (eg. not in unit tests)
            this.fwVersion = GBApplication.getContext().getString(R.string.n_a);
            this.hwVersion = GBApplication.getContext().getString(R.string.n_a);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "fwVersion: " + fwVersion + "; hwVersion: " + hwVersion;
    }
}

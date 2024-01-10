/*  Copyright (C) 2020-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Data;

import java.io.Serializable;

public class CaptureGroup implements Serializable {
    private int index;
    private int flownCurrent;
    private int flownWattage;

    public CaptureGroup(int index, int flownCurrent, int flownWattage) {
        this.flownCurrent = flownCurrent;
        this.flownWattage = flownWattage;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getFlownCurrent() {
        return flownCurrent;
    }

    public void setFlownCurrent(int flownCurrent) {
        this.flownCurrent = flownCurrent;
    }

    public int getFlownWattage() {
        return flownWattage;
    }

    public void setFlownWattage(int flownWattage) {
        this.flownWattage = flownWattage;
    }
}

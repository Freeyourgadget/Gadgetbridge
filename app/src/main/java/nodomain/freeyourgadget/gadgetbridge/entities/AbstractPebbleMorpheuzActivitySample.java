/*  Copyright (C) 2016-2018 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.entities;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public abstract class AbstractPebbleMorpheuzActivitySample extends AbstractActivitySample {

    @Override
    public int getKind() {
        int rawIntensity = getRawIntensity();
        if (rawIntensity <= 120) {
            return ActivityKind.TYPE_DEEP_SLEEP;
        } else if (rawIntensity <= 1000) {
            return ActivityKind.TYPE_LIGHT_SLEEP;
        }
        return ActivityKind.TYPE_ACTIVITY;
    }
}

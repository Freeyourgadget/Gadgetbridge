/*  Copyright (C) 2016-2019 Andreas Shimokawa

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

public abstract class AbstractPebbleMisfitActivitySample extends AbstractActivitySample {
    abstract public int getRawPebbleMisfitSample();

    private transient int intensity = 0;
    private transient int steps = 0;
    private transient int activityKind = ActivityKind.TYPE_UNKNOWN;

    private void calculate() {
        int sample = getRawPebbleMisfitSample();

        if (((sample & 0x83ff) == 0x0001) && ((sample & 0xff00) <= 0x4800)) {
            // sleep seems to be from 0x2401 to 0x4801  (0b0IIIII0000000001) where I = intensity ?
            intensity = (sample & 0x7c00) >>> 10;
            // 9-18 decimal after shift
            if (intensity <= 13) {
                activityKind = ActivityKind.TYPE_DEEP_SLEEP;
            } else {
                // FIXME: this leads to too much false positives, ignore for now
                //activityKind = ActivityKind.TYPE_LIGHT_SLEEP;
                //intensity *= 2; // better visual distinction
            }
        } else {
            if ((sample & 0x0001) == 0) { // 16-??? steps encoded in bits 1-7
                steps = (sample & 0x00fe);
            } else { // 0-14 steps encoded in bits 1-3, most of the time fc71 bits are set in that case
                steps = (sample & 0x000e);
            }
            intensity = steps;
            activityKind = ActivityKind.TYPE_ACTIVITY;
        }
    }

    @Override
    public int getSteps() {
        calculate();
        return steps;
    }

    @Override
    public int getKind() {
        calculate();
        return activityKind;
    }

    @Override
    public int getRawIntensity() {
        calculate();
        return intensity;
    }
}
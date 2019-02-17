/*  Copyright (C) 2015-2019 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.impl;

import nodomain.freeyourgadget.gadgetbridge.model.SummaryOfDay;

public class GBSummaryOfDay implements SummaryOfDay {
    private byte provider;
    private int steps;
    private int dayStartWakeupTime;
    private int dayEndFallAsleepTime;

    public byte getProvider() {
        return provider;
    }

    public int getSteps() {
        return steps;
    }

    public int getDayStartWakeupTime() {
        return dayStartWakeupTime;
    }

    public int getDayEndFallAsleepTime() {
        return dayEndFallAsleepTime;
    }


}

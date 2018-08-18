/*  Copyright (C) 2015-2018 Carsten Pfeiffer

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

import java.util.ArrayList;
import java.util.List;

public class ActivityAmounts {
    private final List<ActivityAmount> amounts = new ArrayList<>(4);
    private long totalSeconds;

    public void addAmount(ActivityAmount amount) {
        amounts.add(amount);
        totalSeconds += amount.getTotalSeconds();
    }

    public List<ActivityAmount> getAmounts() {
        return amounts;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public void calculatePercentages() {
        for (ActivityAmount amount : amounts) {
            float fraction = amount.getTotalSeconds() / (float) totalSeconds;
            amount.setPercent((short) (fraction * 100));
        }
    }
}

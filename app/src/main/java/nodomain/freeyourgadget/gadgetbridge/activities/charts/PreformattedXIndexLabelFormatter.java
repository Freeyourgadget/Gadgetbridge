/*  Copyright (C) 2015-2020 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, Dikay900, Pavel Elagin, vanous, walkjivefly

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;

class PreformattedXIndexLabelFormatter extends ValueFormatter {
    private final ArrayList<String> xLabels;

    public PreformattedXIndexLabelFormatter(ArrayList<String> xLabels) {
        this.xLabels = xLabels;
    }

    @Override
    public String getFormattedValue(float value) {
        int index = (int) value;
        if (xLabels == null || index >= xLabels.size()) {
            return String.valueOf(value);
        }
        return xLabels.get(index);
    }
}

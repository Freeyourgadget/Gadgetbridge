/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.formatter.ValueFormatter;

public class DefaultChartsData<T extends ChartData<?>> extends ChartsData {
    private final T data;
    private final ValueFormatter xValueFormatter;

    public DefaultChartsData(final T data, final ValueFormatter xValueFormatter) {
        this.xValueFormatter = xValueFormatter;
        this.data = data;
    }

    public ValueFormatter getXValueFormatter() {
        return xValueFormatter;
    }

    public T getData() {
        return data;
    }
}

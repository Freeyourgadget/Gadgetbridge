/*  Copyright (C) 2017 Vebryn

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

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by nhu on 30/04/17.
 */

public class XAxisValueFormatter implements IAxisValueFormatter {
    private List<String> mValues = new ArrayList<>();

    public XAxisValueFormatter() {
        super();
    }

    public void add(String label) {
        mValues.add(label);
    }

    public void sort() {
        //System.out.println("Sorting " + mValues);
        Collections.sort(mValues);
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        String returnString = "N/A";

        try {
            returnString = mValues.get((int) value).toString();
            //System.out.println("Asking " + value + ", returning " + returnString);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return returnString;
    }
}

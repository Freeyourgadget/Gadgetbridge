package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Created by Vebryn on 30/04/17.
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
        GB.log("Sorting " + mValues, GB.INFO, null);
        Collections.sort(mValues);
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        String returnString = "N/A";

        try {
            returnString = mValues.get((int) value).toString();
            //System.out.println("Asking " + value + ", returning " + returnString);
            GB.log("Asking " + value + ", returning " + returnString, GB.INFO, null);
        } catch (Exception e) {
            //System.out.println(e.getMessage());
            GB.log(e.getMessage(), GB.ERROR, null);
        }
        return returnString;
    }
}

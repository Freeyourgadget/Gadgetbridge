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

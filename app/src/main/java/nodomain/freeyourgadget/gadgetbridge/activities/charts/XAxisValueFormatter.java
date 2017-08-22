package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Vebryn on 30/04/17.
 */

public class XAxisValueFormatter implements IAxisValueFormatter {
    private static final Logger LOG = LoggerFactory.getLogger(XAxisValueFormatter.class);
    private List<String> mValues = new ArrayList<>();

    public XAxisValueFormatter() {
        super();
    }

    public void add(String label) {
        mValues.add(label);
    }

    public void sort() {
        LOG.info("Sorting " + mValues);
        Collections.sort(mValues);
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        String returnString = "N/A";

        try {
            returnString = mValues.get((int) value).toString();
            LOG.info("Asking " + value + ", returning " + returnString);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return returnString;
    }
}

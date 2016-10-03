package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import java.util.ArrayList;

public abstract class ChartsData {
    private ArrayList<String> xLabels;

    public void setxLabels(ArrayList<String> xLabels) {
        this.xLabels = xLabels;
    }

    public ArrayList<String> getXLabels() {
        return xLabels;
    }
}

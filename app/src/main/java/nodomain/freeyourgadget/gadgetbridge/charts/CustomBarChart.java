package nodomain.freeyourgadget.gadgetbridge.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.BarChart;

/**
 * A BarChart with some specific customization, like
 * <li>using a custom legend renderer that always uses fixed labels and colors</li>
 */
public class CustomBarChart extends BarChart {

    public CustomBarChart(Context context) {
        super(context);
    }

    public CustomBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();
        mLegendRenderer = new CustomLegendRenderer(getViewPortHandler(), getLegend());
    }
}

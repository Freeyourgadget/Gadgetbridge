package nodomain.freeyourgadget.gadgetbridge.charts;

import android.graphics.Typeface;

import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.renderer.LegendRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * A legend renderer that does *not* calculate the labels and colors automatically
 * from the data sets or the data entries.
 * <p/>
 * Instead, they have to be provided manually, because otherwise the legend will
 * be empty.
 */
public class CustomLegendRenderer extends LegendRenderer {
    public CustomLegendRenderer(ViewPortHandler viewPortHandler, Legend legend) {
        super(viewPortHandler, legend);
    }

    @Override
    public void computeLegend(ChartData<?> data) {
        if (!mLegend.isEnabled()) {
            return;
        }

        // don't call super to avoid computing colors and labels
        // super.computeLegend(data);

        Typeface tf = mLegend.getTypeface();

        if (tf != null)
            mLegendLabelPaint.setTypeface(tf);

        mLegendLabelPaint.setTextSize(mLegend.getTextSize());
        mLegendLabelPaint.setColor(mLegend.getTextColor());

        // calculate all dimensions of the mLegend
        mLegend.calculateDimensions(mLegendLabelPaint);
    }
}

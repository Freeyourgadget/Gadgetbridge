package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Canvas;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class AngledLabelsChartRenderer extends BarChartRenderer {
    public AngledLabelsChartRenderer(BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
    }

    @Override
    public void drawValue(Canvas canvas, IValueFormatter formatter, float value, Entry entry, int dataSetIndex, float x, float y, int color) {

        mValuePaint.setColor(color);

        //move position to the center of bar
        x=x+8;
        y=y-25;

        canvas.save();
        canvas.rotate(-90, x, y);

        canvas.drawText(formatter.getFormattedValue(value, entry, dataSetIndex, mViewPortHandler), x, y, mValuePaint);
        canvas.restore();
    }}

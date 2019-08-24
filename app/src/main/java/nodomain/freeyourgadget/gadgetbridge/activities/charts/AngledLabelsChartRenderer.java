package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Canvas;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class AngledLabelsChartRenderer extends BarChartRenderer {
    AngledLabelsChartRenderer(BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
    }

    @Override
    public void drawValue(Canvas canvas, String valueText, float x, float y, int color) {

        mValuePaint.setColor(color);

        //move position to the center of bar
        x=x+8;
        y=y-25;

        canvas.save();
        canvas.rotate(-90, x, y);

        canvas.drawText(valueText, x, y, mValuePaint);

        canvas.restore();
    }}

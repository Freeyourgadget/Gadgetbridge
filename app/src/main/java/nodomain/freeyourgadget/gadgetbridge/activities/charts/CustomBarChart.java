package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.renderer.BarChartRenderer;

/**
 * A BarChart with some specific customization, like
 * <li>allowing to animate a single entry's values without going over 0</li>
 */
public class CustomBarChart extends BarChart {

    private Entry entry = null;
    private SingleEntryValueAnimator singleEntryAnimator;

    public CustomBarChart(Context context) {
        super(context);
    }

    public CustomBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setSinglAnimationEntry(Entry entry) {
        this.entry = entry;

        if (entry != null) {
            // single entry animation mode
            singleEntryAnimator = new SingleEntryValueAnimator(entry, new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    // ViewCompat.postInvalidateOnAnimation(Chart.this);
                    postInvalidate();
                }
            });
            mAnimator = singleEntryAnimator;
            mRenderer = new BarChartRenderer(this, singleEntryAnimator, getViewPortHandler());
        }
    }

    /**
     * Call this to set the next value for the Entry to be animated.
     * Call animateY() when ready to do that.
     * @param nextValue
     */
    public void setSingleEntryYValue(float nextValue) {
        if (singleEntryAnimator != null) {
            singleEntryAnimator.setEntryYValue(nextValue);
        }
    }
}

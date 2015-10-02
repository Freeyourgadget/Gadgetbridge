package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.data.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleEntryValueAnimator extends ChartAnimator {
    private static final Logger LOG = LoggerFactory.getLogger(SingleEntryValueAnimator.class);

    private final Entry entry;
    private final ValueAnimator.AnimatorUpdateListener listener;
    private float previousValue;

    public SingleEntryValueAnimator(Entry singleEntry, ValueAnimator.AnimatorUpdateListener listener) {
        super(listener);
        this.listener = listener;
        entry = singleEntry;
    }

    public void setEntryYValue(float value) {
        this.previousValue = entry.getVal();
        entry.setVal(value);
    }

    @Override
    public void animateY(int durationMillis) {
        // we start with the previous value and animate the change to the
        // next value.
        // as our animation values are not used as absolute values, but as factors,
        // we have to calculate the proper factors in advance. The entry already has
        // the new value, so we create a factor to calculate the old value from the
        // new value.

        float startAnim;
        float endAnim = 1f;
        if (entry.getVal() == 0f) {
            startAnim = 0f;
        } else {
            startAnim = previousValue / entry.getVal();
        }

//        LOG.debug("anim factors: " + startAnim + ", " + endAnim);

        ObjectAnimator animatorY = ObjectAnimator.ofFloat(this, "phaseY", startAnim, endAnim);
        animatorY.setDuration(durationMillis);
        animatorY.addUpdateListener(listener);
        animatorY.start();
    }
}

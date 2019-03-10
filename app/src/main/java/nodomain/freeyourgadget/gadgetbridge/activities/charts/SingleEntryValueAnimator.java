/*  Copyright (C) 2015-2019 Carsten Pfeiffer

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

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
        this.previousValue = entry.getY();
        entry.setY(value);
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
        if (entry.getY() == 0f) {
            startAnim = 0f;
        } else {
            startAnim = previousValue / entry.getY();
        }

//        LOG.debug("anim factors: " + startAnim + ", " + endAnim);

        ObjectAnimator animatorY = ObjectAnimator.ofFloat(this, "phaseY", startAnim, endAnim);
        animatorY.setDuration(durationMillis);
        animatorY.addUpdateListener(listener);
        animatorY.start();
    }
}

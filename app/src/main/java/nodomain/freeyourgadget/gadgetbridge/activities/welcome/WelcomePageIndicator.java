/*  Copyright (C) 2024 Arjan Schrijver

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.welcome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import nodomain.freeyourgadget.gadgetbridge.R;

public class WelcomePageIndicator extends View {
    private ViewPager2 viewPager;
    private int pageCount;
    private int dotRadius = 15;
    private int color;

    private Paint outlinePaint;
    private Paint filledPaint;
    private float currentX = 0.0f;

    private ValueAnimator dotAnimator;

    public WelcomePageIndicator(Context context) {
        super(context);
        init();
    }

    public WelcomePageIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        determineColor(context, attrs);
        init();
    }

    public WelcomePageIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        determineColor(context, attrs);
        init();
    }

    private void determineColor(Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WelcomePageIndicator);
        color = a.getColor(R.styleable.WelcomePageIndicator_page_indicator_color, Color.BLACK);
        a.recycle();
    }

    private void init() {
        outlinePaint = new Paint();
        outlinePaint.setColor(color);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(4);
        outlinePaint.setAntiAlias(true);
        filledPaint = new Paint();
        filledPaint.setColor(color);
        filledPaint.setStyle(Paint.Style.FILL);
        outlinePaint.setAntiAlias(true);
    }

    public void setViewPager(ViewPager2 viewPager) {
        this.viewPager = viewPager;
        this.pageCount = viewPager.getAdapter().getItemCount();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                animateIndicator(position);
            }
        });
        invalidate();
    }

    private int getHorizontalMargin() {
        int dotDiameter = dotRadius * 2;
        int dotSpaces = pageCount * 2 - 1;
        return (getWidth() - dotSpaces * dotDiameter) / 2 + dotRadius;
    }

    private void animateIndicator(int position) {
        float horizontalMargin = getHorizontalMargin();
        if (horizontalMargin <= 0.0f) {
            // Not animating because the drawable is not ready yet
            return;
        }
        float targetX = horizontalMargin + 4 * dotRadius * position;
        if (dotAnimator != null && dotAnimator.isRunning()) {
            dotAnimator.cancel();
        }
        if (currentX == 0.0f) currentX = horizontalMargin;
        dotAnimator = ValueAnimator.ofFloat(currentX, targetX);
        dotAnimator.addUpdateListener(animation -> {
            currentX = (float) animation.getAnimatedValue();
            invalidate();
        });
        dotAnimator.setDuration(300);
        dotAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (viewPager == null || pageCount == 0) {
            return;
        }

        float horizontalMargin = getHorizontalMargin();
        if (currentX == 0.0f && horizontalMargin != 0.0f) currentX = horizontalMargin;
        float circleY = getHeight() / 2f;
        for (int i = 0; i < pageCount; i++) {
            float circleX = horizontalMargin + 4 * dotRadius * i;
            canvas.drawCircle(circleX, circleY, dotRadius, outlinePaint);
        }
        canvas.drawCircle(currentX, circleY, dotRadius, filledPaint);
    }
}

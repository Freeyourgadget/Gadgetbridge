/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.devices.withingssteelhr;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import nodomain.freeyourgadget.gadgetbridge.R;

public class RotaryControl extends View {

    public interface RotationListener {
        void onRotation(short movementAmount);
    }

    private Path circlePath;

    private int controlPointX;
    private int controlPointY;

    private int controlCenterX;
    private int controlCenterY;
    private int controlRadius;

    private int padding;
    private int controlPointSize;
    private int controlPointColor;
    private int lineColor;
    private int lineThickness;
    private double startAngle;
    private double angle ;
    private boolean isControlPointSelected = false;
    private Paint paint = new Paint();
    private Paint controlPointPaint = new Paint();
    private RotationListener rotationListener;

    public RotaryControl(Context context) {
        this(context, null);
    }

    public RotaryControl(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotaryControl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RotaryControl, defStyleAttr, 0);

        startAngle = a.getFloat(R.styleable.RotaryControl_start_angle, (float) Math.PI / 2);
        angle = startAngle;
        controlPointSize = a.getDimensionPixelSize(R.styleable.RotaryControl_controlpoint_size, 50);
        controlPointColor = a.getColor(R.styleable.RotaryControl_controlpoint_color, Color.GRAY);
        lineThickness = a.getDimensionPixelSize(R.styleable.RotaryControl_line_thickness, 20);
        lineColor = a.getColor(R.styleable.RotaryControl_line_color, Color.RED);
        calculateAndSetPadding();
        a.recycle();
    }

    private void calculateAndSetPadding() {
        int totalPadding = getPaddingLeft() + getPaddingRight() + getPaddingBottom() + getPaddingTop() + getPaddingEnd() + getPaddingStart();
        padding = totalPadding / 6;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        int smallerDim = width > height ? height : width;
        int largestCenteredSquareLeft = (width - smallerDim) / 2;
        int largestCenteredSquareTop = (height - smallerDim) / 2;
        int largestCenteredSquareRight = largestCenteredSquareLeft + smallerDim;
        int largestCenteredSquareBottom = largestCenteredSquareTop + smallerDim;
        controlCenterX = largestCenteredSquareRight / 2 + (width - largestCenteredSquareRight) / 2;
        controlCenterY = largestCenteredSquareBottom / 2 + (height - largestCenteredSquareBottom) / 2;
        controlRadius = smallerDim / 2 - lineThickness / 2 - padding;

        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawRotationCircle(canvas);
        drawControlPoint(canvas);

    }

    private void drawControlPoint(Canvas canvas) {
        controlPointX = (int) (controlCenterX + controlRadius * Math.cos(angle));
        controlPointY = (int) (controlCenterY - controlRadius * Math.sin(angle));
        controlPointPaint.setColor(controlPointColor);
        controlPointPaint.setStyle(Paint.Style.FILL);
        controlPointPaint.setAlpha(128);
        Path controlPointPath = new Path();
        controlPointPath.addCircle(controlPointX, controlPointY, controlPointSize, Path.Direction.CW);
        canvas.drawPath(controlPointPath, controlPointPaint);
    }

    private void drawRotationCircle(Canvas canvas) {
        DashPathEffect dashPath = new DashPathEffect(new float[]{8,22}, (float)1.0);
        paint.setPathEffect(dashPath);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(lineThickness);
        paint.setAntiAlias(true);
        paint.setColor(lineColor);
        circlePath = new Path();
        circlePath.addCircle(controlCenterX, controlCenterY, controlRadius, Path.Direction.CW);
        canvas.drawPath(circlePath, paint);
    }

    private void updateRotationPosition(double touchX, double touchY) {
        double distanceX = touchX - controlCenterX;
        double distanceY = controlCenterY - touchY;
        double c = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
        double currentAngle = Math.acos(distanceX / c);
        if (distanceY < 0) {
            currentAngle = -currentAngle;
        }

        int movementAmount = (int) ((currentAngle - angle) * 100);

        int i = (int) movementAmount;
        if (movementAmount != 0) {
            if (Math.abs(movementAmount) > 15) {
                movementAmount /= movementAmount;
            }

            rotationListener.onRotation((short) -movementAmount);
        }

        angle = currentAngle;
    }

    public void setRotationListener(RotationListener listener) {
        rotationListener = listener;
    }

    public void reset() {
        angle = startAngle;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                double x = ev.getX();
                double y = ev.getY();
                if (x < controlPointX + controlPointSize && x > controlPointX - controlPointSize && y < controlPointY + controlPointSize && y > controlPointY - controlPointSize) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    isControlPointSelected = true;
                    updateRotationPosition(x, y);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (isControlPointSelected) {
                    double x = ev.getX();
                    double y = ev.getY();
                    updateRotationPosition(x, y);
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                getParent().requestDisallowInterceptTouchEvent(false);
                isControlPointSelected = false;
                break;
            }
        }

        invalidate();
        return true;
    }
}

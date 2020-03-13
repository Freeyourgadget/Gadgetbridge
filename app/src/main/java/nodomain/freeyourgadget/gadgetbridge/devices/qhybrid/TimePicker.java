/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public class TimePicker extends AlertDialog.Builder {
    ImageView pickerView;
    Canvas pickerCanvas;
    Bitmap pickerBitmap;

    NotificationConfiguration settings;

    int height, width, radius;
    int radius1, radius2, radius3;
    int controlledHand = 0;
    int handRadius;

    AlertDialog dialog;

    OnFinishListener finishListener;
    OnHandsSetListener handsListener;
    OnVibrationSetListener vibrationListener;

    protected TimePicker(@NonNull Context context, PackageInfo info) {
        super(context);

        settings = new NotificationConfiguration(info.packageName, context.getApplicationContext().getPackageManager().getApplicationLabel(info.applicationInfo).toString());
        initGraphics(context);
    }

    protected TimePicker(Context context, NotificationConfiguration config){
        super(context);

        settings = config;
        initGraphics(context);
    }

    private void initGraphics(Context context){
        int w = (int) (((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth() * 0.8);
        height = w;
        width = w;
        radius = (int) (w * 0.06);
        radius1 = 0;
        radius2 = (int) (radius * 2.3);
        radius3 = (int)(radius2 * 2.15);
        int offset = (int) (w * 0.1);
        radius1 += offset;
        radius2 += offset;
        radius3 += offset;

        pickerView = new ImageView(context);
        pickerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        pickerCanvas = new Canvas(pickerBitmap);

        drawClock();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(pickerView);

        CheckBox box = new CheckBox(context);
        box.setText("Respect silent mode");
        box.setChecked(settings.getRespectSilentMode());
        box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settings.setRespectSilentMode(b);
            }
        });
        layout.addView(box);

        RadioGroup group = new RadioGroup(context);
        for(PlayNotificationRequest.VibrationType vibe: PlayNotificationRequest.VibrationType.values()){
            RadioButton button = new RadioButton(context);
            button.setText(vibe.toString());
            button.setId(vibe.getValue());
            group.addView(button);
        }

        group.check(settings.getVibration().getValue());
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                settings.setVibration(PlayNotificationRequest.VibrationType.fromValue((byte)i));
                if(TimePicker.this.vibrationListener != null) TimePicker.this.vibrationListener.onVibrationSet(settings);
            }
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(group);
        layout.addView(scrollView);

        setView(layout);



        setNegativeButton("cancel", null);
        setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(finishListener == null) return;
                finishListener.onFinish(true, settings);
            }
        });
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if(finishListener == null) return;
                finishListener.onFinish(false, settings);
            }
        });
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if(finishListener == null) return;
                finishListener.onFinish(false, settings);
            }
        });
        dialog = show();

        pickerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                handleTouch(dialog, motionEvent);
                return true;
            }
        });
    }

    public NotificationConfiguration getSettings() {
        return settings;
    }

    private void handleTouch(AlertDialog dialog, MotionEvent event) {
        int centerX = width / 2;
        int centerY = height / 2;
        int difX = centerX - (int) event.getX();
        int difY = (int) event.getY() - centerY;
        int dist = (int) Math.sqrt(Math.abs(difX) * Math.abs(difX) + Math.abs(difY) * Math.abs(difY));
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                int radiusHalf = radius;
                if (dist < (radius1 + radiusHalf) && dist > (radius1 - radiusHalf)) {
                    Log.d("Settings", "hit sub");
                    handRadius = (int) (height / 2f - radius1);
                    controlledHand = 3;
                } else if (dist < (radius2 + radiusHalf) && dist > (radius2 - radiusHalf)) {
                    Log.d("Settings", "hit hour");
                    controlledHand = 1;
                    handRadius = (int) (height / 2f - radius2);
                } else if (dist < (radius3 + radiusHalf) && dist > (radius3 - radiusHalf)) {
                    Log.d("Settings", "hit minute");
                    controlledHand = 2;
                    handRadius = (int) (height / 2f - radius3);
                } else {
                    Log.d("Settings", "hit nothing");
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (controlledHand == 0) return;
                double degree = difY == 0 ? (difX < 0 ? 90 : 270) : Math.toDegrees(Math.atan((float) difX / (float) difY));
                if (difY > 0) degree = 180 + degree;
                if (degree < 0) degree = 360 + degree;
                switch (controlledHand) {
                    case 1: {
                        settings.setHour((short) (((int)(degree + 15) / 30) * 30 % 360));
                        break;
                    }
                    case 2: {
                        settings.setMin((short) (((int)(degree + 15) / 30) * 30 % 360));
                        break;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(true);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAlpha(1f);
                if(handsListener != null) handsListener.onHandsSet(settings);
                break;
            }
        }
        drawClock();
    }


    private void drawClock() {
        //pickerCanvas.drawColor(Color.WHITE);
        Paint white = new Paint();
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        pickerCanvas.drawCircle(width / 2, width / 2, width / 2, white);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.BLUE);
        Paint text = new Paint();
        text.setStyle(Paint.Style.FILL);
        text.setTextSize(radius * 1.5f);
        text.setColor(Color.BLACK);
        text.setTextAlign(Paint.Align.CENTER);
        int textShiftY = (int) ((text.descent() + text.ascent()) / 2);

        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(10);
        linePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        linePaint.setColor(Color.BLACK);

        if (settings.getMin() != -1) {
            paint.setAlpha(255);
            float x = (float) (width / 2f + Math.sin(Math.toRadians(settings.getMin())) * (float) radius3);
            float y = (float) (height / 2f - Math.cos(Math.toRadians(settings.getMin())) * (float) radius3);
            linePaint.setAlpha(255);
            pickerCanvas.drawLine(width / 2, height / 2, x, y, linePaint);
            pickerCanvas.drawCircle(
                    x,
                    y,
                    radius,
                    paint
            );
            pickerCanvas.drawText(String.valueOf(settings.getMin() / 6), x, y - textShiftY, text);
        }
        if (settings.getHour() != -1) {
            paint.setAlpha(255);
            float x = (float) (width / 2f + Math.sin(Math.toRadians(settings.getHour())) * (float) radius2);
            float y = (float) (height / 2f - Math.cos(Math.toRadians(settings.getHour())) * (float) radius2);
            linePaint.setAlpha(255);
            pickerCanvas.drawLine(width / 2, height / 2, x, y, linePaint);
            pickerCanvas.drawCircle(
                    x,
                    y,
                    radius,
                    paint
            );
            pickerCanvas.drawText(settings.getHour() == 0 ? "12" : String.valueOf(settings.getHour() / 30), x, y - textShiftY, text);
        }

        Paint paint2 = new Paint();
        paint2.setColor(Color.BLACK);
        paint2.setStyle(Paint.Style.FILL_AND_STROKE);
        pickerCanvas.drawCircle(width / 2, height / 2, 5, paint2);
        pickerView.setImageBitmap(pickerBitmap);
    }

    interface OnFinishListener{
        public void onFinish(boolean success, NotificationConfiguration config);
    }

    interface OnHandsSetListener{
        public void onHandsSet(NotificationConfiguration config);
    }

    interface OnVibrationSetListener{
        public void onVibrationSet(NotificationConfiguration config);
    }
}

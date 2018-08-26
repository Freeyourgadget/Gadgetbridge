package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.PlayNotificationRequest;

public class TimePicker extends AlertDialog.Builder {
    ImageView pickerView;
    Canvas pickerCanvas;
    Bitmap pickerBitmap;

    PackageConfig settings;

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

        settings = new PackageConfig(info.packageName, context.getApplicationContext().getPackageManager().getApplicationLabel(info.applicationInfo).toString());
        initGraphics(context);
    }

    protected TimePicker(Context context, PackageConfig config){
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
        box.setOnCheckedChangeListener((compoundButton, b) -> settings.setRespectSilentMode(b));
        layout.addView(box);

        RadioGroup group = new RadioGroup(context);
        for(PlayNotificationRequest.VibrationType vibe: PlayNotificationRequest.VibrationType.values()){
            RadioButton button = new RadioButton(context);
            button.setText(vibe.toString());
            button.setId(vibe.getValue());
            group.addView(button);
        }

        group.check(settings.getVibration());
        group.setOnCheckedChangeListener((radioGroup, i) -> {
            settings.setVibration(i);
            if(this.vibrationListener != null) this.vibrationListener.onVibrationSet(settings);
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(group);
        layout.addView(scrollView);

        setView(layout);



        setNegativeButton("cancel", null);
        setPositiveButton("ok", (dialogInterface, i) -> {
            if(finishListener == null) return;
            finishListener.onFinish(true, settings);
        });
        setOnCancelListener((a) -> {
            if(finishListener == null) return;
            finishListener.onFinish(false, settings);
        });
        setOnDismissListener((a) -> {
            if(finishListener == null) return;
            finishListener.onFinish(false, settings);
        });
        dialog = show();
        if(this.settings.getHour() == -1 && this.settings.getMin() == -1) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAlpha(0.4f);
        }

        pickerView.setOnTouchListener((view, motionEvent) -> {
            handleTouch(dialog, motionEvent);
            return true;
        });
    }

    public PackageConfig getSettings() {
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
                        settings.setHour((short) (((degree + 15) / 30) * 30 % 360));
                        break;
                    }
                    case 2: {
                        settings.setMin((short) (((degree + 15) / 30) * 30 % 360));
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

        if (settings.getMin() == -1) {
            linePaint.setAlpha(100);
            pickerCanvas.drawLine(width / 2, height / 2, width / 2, height / 2f - radius3, linePaint);

            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            pickerCanvas.drawCircle(width / 2f, height / 2f - radius3, radius, paint);
            paint.setAlpha(100);
            paint.setColor(Color.BLUE);
            pickerCanvas.drawCircle(width / 2f, height / 2f - radius3, radius, paint);
        } else {
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
        if (settings.getHour() == -1) {
            paint.setAlpha(100);
            if (settings.getMin() != -1) {
                linePaint.setAlpha(100);
                pickerCanvas.drawLine(width / 2, height / 2, width / 2, height / 2f - radius2, linePaint);
            }
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            pickerCanvas.drawCircle(width / 2f, height / 2f - radius2, radius, paint);
            paint.setAlpha(100);
            paint.setColor(Color.BLUE);
            pickerCanvas.drawCircle(width / 2f, height / 2f - radius2, radius, paint);
        } else {
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
        public void onFinish(boolean success, PackageConfig config);
    }

    interface OnHandsSetListener{
        public void onHandsSet(PackageConfig config);
    }

    interface OnVibrationSetListener{
        public void onVibrationSet(PackageConfig config);
    }
}
